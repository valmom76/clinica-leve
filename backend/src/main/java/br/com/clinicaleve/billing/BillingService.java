package br.com.clinicaleve.billing;

import br.com.clinicaleve.billing.BillingDtos.BillingOverviewResponse;
import br.com.clinicaleve.billing.BillingDtos.BillingProfileRequest;
import br.com.clinicaleve.billing.BillingDtos.BillingProfileResponse;
import br.com.clinicaleve.billing.BillingDtos.PaymentResponse;
import br.com.clinicaleve.billing.BillingDtos.PlanResponse;
import br.com.clinicaleve.billing.BillingDtos.StartSubscriptionRequest;
import br.com.clinicaleve.billing.BillingDtos.StartSubscriptionResponse;
import br.com.clinicaleve.billing.BillingDtos.SubscriptionResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillingService {
    public static final int GRACE_PERIOD_DAYS = 7;

    private static final Set<SubscriptionStatus> RESERVED_PLAN_STATUSES = Set.of(
            SubscriptionStatus.PENDING,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAST_DUE,
            SubscriptionStatus.SUSPENDED
    );

    private final SubscriptionPlanRepository planRepository;
    private final ClinicBillingProfileRepository profileRepository;
    private final ClinicSubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionProvisioningService provisioningService;
    private final SubscriptionPaymentService paymentService;
    private final AsaasClient asaasClient;
    private final AsaasProperties asaasProperties;

    @Transactional
    public BillingOverviewResponse overview() {
        return overviewFor(TenantAccess.currentClinicId());
    }

    @Transactional
    public BillingProfileResponse saveProfile(BillingProfileRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var document = digits(request.cpfCnpj());
        if (document.length() != 11 && document.length() != 14) {
            throw new IllegalArgumentException("Informe um CPF ou CNPJ válido");
        }
        var phone = digits(request.phone());
        if (phone.length() < 10 || phone.length() > 13) {
            throw new IllegalArgumentException("Informe um telefone válido com DDD");
        }
        var postalCode = nullableDigits(request.postalCode());
        if (postalCode != null && postalCode.length() != 8) {
            throw new IllegalArgumentException("Informe um CEP válido com 8 dígitos");
        }

        var profile = profileRepository.findByClinicId(clinicId).orElseGet(ClinicBillingProfile::new);
        profile.setClinicId(clinicId);
        profile.setLegalName(request.legalName().trim());
        profile.setCpfCnpj(document);
        profile.setEmail(request.email().trim().toLowerCase());
        profile.setPhone(phone);
        profile.setPostalCode(postalCode);
        profile.setAddress(nullable(request.address()));
        profile.setAddressNumber(nullable(request.addressNumber()));
        profile.setComplement(nullable(request.complement()));
        profile.setProvince(nullable(request.province()));
        return profileResponse(profileRepository.save(profile));
    }

    public StartSubscriptionResponse start(StartSubscriptionRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var subscription = provisioningService.currentOrCreate(clinicId);
        var plan = planRepository.findByCodeAndActiveTrue(request.planCode().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Plano não encontrado"));

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("A clínica já possui uma assinatura ativa");
        }
        if (subscription.getStatus() == SubscriptionStatus.PENDING
                && plan.getId().equals(subscription.getPlanId())
                && request.paymentMethod() == subscription.getPaymentMethod()
                && subscription.getCheckoutUrl() != null) {
            return new StartSubscriptionResponse(
                    subscriptionResponse(subscription),
                    subscription.getCheckoutUrl(),
                    "A cobrança pendente foi recuperada."
            );
        }
        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            throw new IllegalStateException("Já existe uma contratação pendente. Conclua ou cancele a cobrança atual.");
        }
        ensurePlanAvailable(plan, subscription);

        var profile = profileRepository.findByClinicId(clinicId)
                .orElseThrow(() -> new IllegalStateException("Preencha os dados de faturamento antes de contratar"));
        var customerId = asaasClient.syncCustomer(profile);
        profile.setAsaasCustomerId(customerId);
        profileRepository.save(profile);

        var previousPlanId = subscription.getPlanId();
        var previousStatus = subscription.getStatus();
        var previousCycle = subscription.getBillingCycle();
        var previousAmount = subscription.getAmount();
        try {
            subscription.setPlanId(plan.getId());
            subscription.setStatus(SubscriptionStatus.PENDING);
            subscription.setPaymentMethod(request.paymentMethod());
            subscription.setBillingCycle(plan.getBillingCycle());
            subscription.setAmount(plan.getPrice());
            subscription.setCancelAtPeriodEnd(false);
            subscription.setCanceledAt(null);
            subscription.setGraceEndsAt(null);
            subscription.setAsaasSubscriptionId(null);
            subscription.setAsaasCheckoutId(null);
            subscription.setCheckoutUrl(null);
            subscriptionRepository.save(subscription);

            var firstDueDate = firstDueDate(subscription);
            if (request.paymentMethod() == BillingPaymentMethod.CREDIT_CARD) {
                var checkout = asaasClient.createCardCheckout(subscription, plan, customerId, firstDueDate);
                subscription.setAsaasCheckoutId(checkout.id());
                subscription.setCheckoutUrl(checkout.link());
            } else {
                var remote = asaasClient.createPixSubscription(subscription, plan, customerId, firstDueDate);
                subscription.setAsaasSubscriptionId(remote.id());
                subscription.setNextDueDate(firstDueDate);
                subscriptionRepository.save(subscription);
                synchronizePayments(subscription);
            }
            var saved = subscriptionRepository.save(subscription);
            return new StartSubscriptionResponse(
                    subscriptionResponse(saved),
                    saved.getCheckoutUrl(),
                    request.paymentMethod() == BillingPaymentMethod.CREDIT_CARD
                            ? "Checkout seguro criado no Asaas."
                            : "Assinatura Pix criada. Use o link para acessar a primeira cobrança."
            );
        } catch (RuntimeException exception) {
            subscription.setPlanId(previousPlanId);
            subscription.setStatus(previousStatus);
            subscription.setPaymentMethod(null);
            subscription.setBillingCycle(previousCycle);
            subscription.setAmount(previousAmount);
            subscription.setAsaasSubscriptionId(null);
            subscription.setAsaasCheckoutId(null);
            subscription.setCheckoutUrl(null);
            subscriptionRepository.save(subscription);
            throw exception;
        }
    }

    public BillingOverviewResponse refresh() {
        var clinicId = TenantAccess.currentClinicId();
        var subscription = provisioningService.currentOrCreate(clinicId);
        if (subscription.getAsaasSubscriptionId() != null && asaasProperties.configured()) {
            synchronizePayments(subscription);
        }
        return overviewFor(clinicId);
    }

    public BillingOverviewResponse cancel() {
        var clinicId = TenantAccess.currentClinicId();
        var subscription = provisioningService.currentOrCreate(clinicId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            return overviewFor(clinicId);
        }

        if (subscription.getAsaasSubscriptionId() != null) {
            asaasClient.cancelSubscription(subscription.getAsaasSubscriptionId());
        }

        if (subscription.getStatus() == SubscriptionStatus.ACTIVE && subscription.getNextDueDate() != null) {
            subscription.setCancelAtPeriodEnd(true);
            subscription.setCanceledAt(Instant.now());
        } else {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setCanceledAt(Instant.now());
        }
        subscriptionRepository.save(subscription);
        return overviewFor(clinicId);
    }

    private BillingOverviewResponse overviewFor(String clinicId) {
        var subscription = provisioningService.currentOrCreate(clinicId);
        var profile = profileRepository.findByClinicId(clinicId).map(this::profileResponse).orElse(null);
        var payments = paymentRepository.findByClinicIdOrderByDueDateDescCreatedAtDesc(clinicId)
                .stream()
                .map(this::paymentResponse)
                .toList();
        return new BillingOverviewResponse(
                asaasProperties.configured(),
                asaasProperties.normalizedEnvironment(),
                GRACE_PERIOD_DAYS,
                planRepository.findByActiveTrueAndVisibleTrueOrderByDisplayOrderAsc()
                        .stream().map(this::planResponse).toList(),
                profile,
                subscriptionResponse(subscription),
                payments
        );
    }

    private void synchronizePayments(ClinicSubscription subscription) {
        for (var remote : asaasClient.listSubscriptionPayments(subscription.getAsaasSubscriptionId())) {
            if (remote.id() == null || remote.id().isBlank()) continue;
            paymentService.apply(
                    subscription,
                    eventForStatus(remote.status()),
                    new SubscriptionPaymentService.PaymentData(
                            remote.id(), remote.status(), remote.billingType(), remote.value(), remote.dueDate(),
                            remote.paymentDate(), remote.clientPaymentDate(), remote.invoiceUrl(),
                            remote.bankSlipUrl(), remote.description()
                    )
            );
        }
    }

    private String eventForStatus(String status) {
        if (status == null) return "PAYMENT_UPDATED";
        return switch (status.toUpperCase()) {
            case "RECEIVED" -> "PAYMENT_RECEIVED";
            case "CONFIRMED" -> "PAYMENT_CONFIRMED";
            case "OVERDUE" -> "PAYMENT_OVERDUE";
            case "REFUNDED" -> "PAYMENT_REFUNDED";
            default -> "PAYMENT_UPDATED";
        };
    }

    private LocalDate firstDueDate(ClinicSubscription subscription) {
        var today = LocalDate.now(ZoneOffset.UTC);
        if (subscription.getTrialEndsAt() == null || subscription.getTrialEndsAt().isBefore(Instant.now())) {
            return today;
        }
        var trialDate = subscription.getTrialEndsAt().atZone(ZoneOffset.UTC).toLocalDate();
        return trialDate.isBefore(today) ? today : trialDate;
    }

    private void ensurePlanAvailable(SubscriptionPlan plan, ClinicSubscription subscription) {
        if (plan.getAvailabilityLimit() == null || plan.getId().equals(subscription.getPlanId())) return;
        var reserved = subscriptionRepository.countByPlanIdAndStatusIn(plan.getId(), RESERVED_PLAN_STATUSES);
        if (reserved >= plan.getAvailabilityLimit()) {
            throw new IllegalStateException("As vagas desta condição promocional foram preenchidas");
        }
    }

    private PlanResponse planResponse(SubscriptionPlan plan) {
        Integer remaining = null;
        var available = true;
        if (plan.getAvailabilityLimit() != null) {
            var reserved = subscriptionRepository.countByPlanIdAndStatusIn(plan.getId(), RESERVED_PLAN_STATUSES);
            remaining = Math.max(0, plan.getAvailabilityLimit() - Math.toIntExact(reserved));
            available = remaining > 0;
        }
        return new PlanResponse(
                plan.getCode(), plan.getName(), plan.getDescription(), plan.getBillingCycle(), plan.getPrice(),
                plan.getTrialDays(), plan.getPriceGuaranteeMonths(), plan.getAvailabilityLimit(), remaining, available
        );
    }

    private BillingProfileResponse profileResponse(ClinicBillingProfile profile) {
        return new BillingProfileResponse(
                profile.getLegalName(), profile.getCpfCnpj(), profile.getEmail(), profile.getPhone(),
                profile.getPostalCode(), profile.getAddress(), profile.getAddressNumber(), profile.getComplement(),
                profile.getProvince(), profile.getAsaasCustomerId() != null
        );
    }

    private SubscriptionResponse subscriptionResponse(ClinicSubscription subscription) {
        var plan = planRepository.findById(subscription.getPlanId())
                .orElseThrow(() -> new IllegalStateException("Plano da assinatura não encontrado"));
        return new SubscriptionResponse(
                subscription.getId(), plan.getCode(), plan.getName(), subscription.getStatus(),
                provisioningService.accessMode(subscription), subscription.getPaymentMethod(),
                subscription.getBillingCycle(), subscription.getAmount(), subscription.getTrialEndsAt(),
                subscription.getNextDueDate(), subscription.getGraceEndsAt(), subscription.isCancelAtPeriodEnd(),
                subscription.getCanceledAt(), subscription.getCheckoutUrl(), subscription.getLastPaymentStatus(),
                subscription.getLastPaymentAt()
        );
    }

    private PaymentResponse paymentResponse(SubscriptionPayment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getStatus(), payment.getBillingType(), payment.getValue(),
                payment.getDueDate(), payment.getPaymentDate(), payment.getInvoiceUrl(),
                payment.getBankSlipUrl(), payment.getDescription()
        );
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String nullableDigits(String value) {
        var normalized = digits(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
