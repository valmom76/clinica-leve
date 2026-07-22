package br.com.clinicaleve.billing;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinic_subscriptions")
public class ClinicSubscription extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BillingPaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private Instant trialEndsAt;

    private LocalDate nextDueDate;

    private Instant graceEndsAt;

    @Column(nullable = false)
    private boolean cancelAtPeriodEnd;

    private Instant canceledAt;

    @Column(length = 40, unique = true)
    private String asaasSubscriptionId;

    @Column(length = 80, unique = true)
    private String asaasCheckoutId;

    @Column(length = 500)
    private String checkoutUrl;

    @Column(length = 40)
    private String lastPaymentStatus;

    private Instant lastPaymentAt;
}
