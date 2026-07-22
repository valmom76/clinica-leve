package br.com.clinicaleve.billing;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "subscription_payments")
public class SubscriptionPayment extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String subscriptionId;

    @Column(nullable = false, unique = true, length = 40)
    private String asaasPaymentId;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 30)
    private String billingType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    private LocalDate dueDate;

    private Instant paymentDate;

    @Column(length = 500)
    private String invoiceUrl;

    @Column(length = 500)
    private String bankSlipUrl;

    @Column(length = 500)
    private String description;
}
