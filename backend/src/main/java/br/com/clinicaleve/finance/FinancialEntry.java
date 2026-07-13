package br.com.clinicaleve.finance;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "financial_entries")
public class FinancialEntry extends TenantEntity {
    @Column(nullable = false, length = 36)
    private String categoryId;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private FinancialEntryType type;
    @Column(nullable = false, length = 180)
    private String description;
    @Column(length = 160)
    private String counterparty;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate dueDate;
    private LocalDate paymentDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancialEntryStatus status = FinancialEntryStatus.OPEN;
    @Column(length = 40)
    private String paymentMethod;
    @Column(length = 500)
    private String notes;
    @Column(nullable = false, length = 36)
    private String createdByUserId;
}
