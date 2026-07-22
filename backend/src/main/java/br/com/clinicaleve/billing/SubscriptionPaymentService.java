package br.com.clinicaleve.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService {
    private static final int GRACE_PERIOD_DAYS = 7;

    private final SubscriptionPaymentRepository paymentRepository;
    private final ClinicSubscriptionRepository subscriptionRepository;

    @Transactional
    public SubscriptionPayment apply(
            ClinicSubscription subscription,
            String eventType,
            PaymentData data
    ) {
        var payment = paymentRepository.findByAsaasPaymentId(data.id())
                .orElseGet(SubscriptionPayment::new);
        payment.setClinicId(subscription.getClinicId());
        payment.setSubscriptionId(subscription.getId());
        payment.setAsaasPaymentId(data.id());
        payment.setStatus(defaultString(data.status(), "PENDING"));
        payment.setBillingType(data.billingType());
        payment.setValue(data.value() == null ? BigDecimal.ZERO : data.value());
        payment.setDueDate(data.dueDate());
        payment.setPaymentDate(parseDate(data.paymentDate(), data.clientPaymentDate()));
        payment.setInvoiceUrl(data.invoiceUrl());
        payment.setBankSlipUrl(data.bankSlipUrl());
        payment.setDescription(data.description());
        var saved = paymentRepository.save(payment);

        updateSubscription(subscription, eventType, saved);
        subscriptionRepository.save(subscription);
        return saved;
    }

    private void updateSubscription(
            ClinicSubscription subscription,
            String eventType,
            SubscriptionPayment payment
    ) {
        subscription.setLastPaymentStatus(payment.getStatus());
        if (payment.getInvoiceUrl() != null && subscription.getPaymentMethod() == BillingPaymentMethod.PIX) {
            subscription.setCheckoutUrl(payment.getInvoiceUrl());
        }

        if (isPaid(eventType, payment.getStatus())) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setGraceEndsAt(null);
            subscription.setLastPaymentAt(payment.getPaymentDate() == null ? Instant.now() : payment.getPaymentDate());
            if (payment.getDueDate() != null) {
                subscription.setNextDueDate(subscription.getBillingCycle() == BillingCycle.YEARLY
                        ? payment.getDueDate().plusYears(1)
                        : payment.getDueDate().plusMonths(1));
            }
            return;
        }

        if ("PAYMENT_OVERDUE".equals(eventType) || "OVERDUE".equalsIgnoreCase(payment.getStatus())) {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            var dueDate = payment.getDueDate() == null ? LocalDate.now(ZoneOffset.UTC) : payment.getDueDate();
            subscription.setGraceEndsAt(dueDate.plusDays(GRACE_PERIOD_DAYS)
                    .atTime(23, 59, 59)
                    .toInstant(ZoneOffset.UTC));
            return;
        }

        if ("PAYMENT_DELETED".equals(eventType)
                && subscription.isCancelAtPeriodEnd()
                && subscription.getStatus() == SubscriptionStatus.ACTIVE) {
            return;
        }

        if (isSuspendingEvent(eventType)) {
            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            subscription.setGraceEndsAt(Instant.now());
        }
    }

    private boolean isPaid(String eventType, String status) {
        return "PAYMENT_RECEIVED".equals(eventType)
                || "PAYMENT_CONFIRMED".equals(eventType)
                || "RECEIVED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status);
    }

    private boolean isSuspendingEvent(String eventType) {
        return switch (eventType) {
            case "PAYMENT_REFUNDED", "PAYMENT_DELETED", "PAYMENT_CHARGEBACK_REQUESTED",
                    "PAYMENT_REPROVED_BY_RISK_ANALYSIS", "PAYMENT_CREDIT_CARD_CAPTURE_REFUSED" -> true;
            default -> false;
        };
    }

    private Instant parseDate(String first, String second) {
        var value = first == null || first.isBlank() ? second : first;
        if (value == null || value.isBlank()) return null;
        try {
            if (value.length() == 10) return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
            return LocalDateTime.parse(value.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record PaymentData(
            String id,
            String status,
            String billingType,
            BigDecimal value,
            LocalDate dueDate,
            String paymentDate,
            String clientPaymentDate,
            String invoiceUrl,
            String bankSlipUrl,
            String description
    ) {
    }
}
