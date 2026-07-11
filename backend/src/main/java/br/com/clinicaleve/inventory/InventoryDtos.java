package br.com.clinicaleve.inventory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name
    ) {
    }

    public record CategoryResponse(String id, String name) {
        static CategoryResponse from(MaterialCategory category) {
            return new CategoryResponse(category.getId(), category.getName());
        }
    }

    public record MaterialRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank String categoryId,
            @Size(max = 80) String sku,
            @NotBlank @Size(max = 30) String unit,
            @NotNull @DecimalMin("0.000") BigDecimal minimumStock,
            boolean lotControlled
    ) {
    }

    public record MaterialResponse(
            String id,
            String name,
            String categoryId,
            String categoryName,
            String sku,
            String unit,
            BigDecimal minimumStock,
            BigDecimal currentStock,
            boolean lotControlled,
            boolean lowStock,
            LocalDate nearestExpiration,
            boolean active
    ) {
        static MaterialResponse from(
                Material material,
                String categoryName,
                LocalDate nearestExpiration
        ) {
            return new MaterialResponse(
                    material.getId(),
                    material.getName(),
                    material.getCategoryId(),
                    categoryName,
                    material.getSku(),
                    material.getUnit(),
                    material.getMinimumStock(),
                    material.getCurrentStock(),
                    material.isLotControlled(),
                    material.getMinimumStock().signum() > 0
                            && material.getCurrentStock().compareTo(material.getMinimumStock()) <= 0,
                    nearestExpiration,
                    material.isActive()
            );
        }
    }

    public record MovementRequest(
            @NotNull StockMovementType type,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 300) String reason,
            @Size(max = 80) String lotNumber,
            LocalDate expirationDate
    ) {
    }

    public record MovementResponse(
            String id,
            StockMovementType type,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            String reason,
            String lotNumber,
            LocalDate expirationDate,
            Instant occurredAt
    ) {
        static MovementResponse from(StockMovement movement, MaterialBatch batch) {
            return new MovementResponse(
                    movement.getId(),
                    movement.getType(),
                    movement.getQuantity(),
                    movement.getBalanceAfter(),
                    movement.getReason(),
                    batch == null ? null : batch.getLotNumber(),
                    batch == null ? null : batch.getExpirationDate(),
                    movement.getOccurredAt()
            );
        }
    }
}
