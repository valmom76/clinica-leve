package br.com.clinicaleve.finance;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "financial_categories")
public class FinancialCategory extends TenantEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private FinancialEntryType type;
    @Column(nullable = false)
    private boolean active = true;
}
