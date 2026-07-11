package br.com.clinicaleve.inventory;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_movements")
public class StockMovement extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String materialId;

    @Column(length = 36)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private StockMovementType type;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(nullable = false, length = 36)
    private String createdByUserId;

    @Column(nullable = false)
    private Instant occurredAt;
}
