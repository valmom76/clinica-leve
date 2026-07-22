package br.com.clinicaleve.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class InventoryReportDtos {

    private InventoryReportDtos() {
    }

    public record MovementReportRow(
            String id,
            String materialId,
            String materialName,
            String unit,
            StockMovementType type,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            String reason,
            String lotNumber,
            String createdByUserName,
            LocalDateTime occurredAt
    ) {
    }

    public record MovementReportResponse(
            LocalDate from,
            LocalDate to,
            String materialId,
            String materialName,
            String unit,
            int movementCount,
            int entryCount,
            int exitCount,
            int distinctMaterials,
            BigDecimal totalEntryQuantity,
            BigDecimal totalExitQuantity,
            List<MovementReportRow> movements
    ) {
    }
}
