package br.com.clinicaleve.billing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class BillingDtos {
    private BillingDtos() {
    }

    public record BillingOverviewResponse(
            boolean billingConfigured,
            String environment,
            int gracePeriodDays,
            List<PlanResponse> plans,
            BillingProfileResponse profile,
            SubscriptionResponse subscription,
            List<PaymentResponse> payments
    ) {
    }

    public record PlanResponse(
            String code,
            String name,
            String description,
            BillingCycle billingCycle,
            BigDecimal price,
            int trialDays,
            Integer priceGuaranteeMonths,
            Integer availabilityLimit,
            Integer remainingSpots,
            boolean available
    ) {
    }

    public record BillingProfileRequest(
            @NotBlank @Size(max = 160) String legalName,
            @NotBlank @Size(max = 18) String cpfCnpj,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(max = 25) String phone,
            @Size(max = 10) String postalCode,
            @Size(max = 180) String address,
            @Size(max = 30) String addressNumber,
            @Size(max = 120) String complement,
            @Size(max = 100) String province
    ) {
    }

    public record BillingProfileResponse(
            String legalName,
            String cpfCnpj,
            String email,
            String phone,
            String postalCode,
            String address,
            String addressNumber,
            String complement,
            String province,
            boolean synchronizedWithAsaas
    ) {
    }

    public record StartSubscriptionRequest(
            @NotBlank String planCode,
            @NotNull BillingPaymentMethod paymentMethod
    ) {
    }

    public record StartSubscriptionResponse(
            SubscriptionResponse subscription,
            String paymentUrl,
            String message
    ) {
    }

    public record SubscriptionResponse(
            String id,
            String planCode,
            String planName,
            SubscriptionStatus status,
            SubscriptionAccessMode accessMode,
            BillingPaymentMethod paymentMethod,
            BillingCycle billingCycle,
            BigDecimal amount,
            Instant trialEndsAt,
            LocalDate nextDueDate,
            Instant graceEndsAt,
            boolean cancelAtPeriodEnd,
            Instant canceledAt,
            String paymentUrl,
            String lastPaymentStatus,
            Instant lastPaymentAt
    ) {
    }

    public record PaymentResponse(
            String id,
            String status,
            String billingType,
            BigDecimal value,
            LocalDate dueDate,
            Instant paymentDate,
            String invoiceUrl,
            String bankSlipUrl,
            String description
    ) {
    }
}
