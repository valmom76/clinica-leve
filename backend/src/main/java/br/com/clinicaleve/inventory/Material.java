package br.com.clinicaleve.inventory;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "materials")
public class Material extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String categoryId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 80)
    private String sku;

    @Column(nullable = false, length = 30)
    private String unit;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal minimumStock = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean lotControlled;

    @Column(nullable = false)
    private boolean active = true;
}
