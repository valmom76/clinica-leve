package br.com.clinicaleve.inventory;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "material_batches")
public class MaterialBatch extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String materialId;

    @Column(nullable = false, length = 80)
    private String lotNumber;

    private LocalDate expirationDate;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;
}
