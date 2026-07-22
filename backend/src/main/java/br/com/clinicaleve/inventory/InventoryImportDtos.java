package br.com.clinicaleve.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class InventoryImportDtos {
    private InventoryImportDtos() {}

    public enum SuggestedAction { CREATE, UPDATE, REVIEW, UNCHANGED, ERROR }
    public enum ImportAction { CREATE, UPDATE, SKIP }

    public record SimilarMaterial(
            String materialId,
            String materialName,
            String sku,
            int similarityPercent
    ) {}

    public record ImportPreviewRow(
            int rowNumber,
            String sourceId,
            String name,
            String categoryName,
            String sku,
            String unit,
            BigDecimal minimumStock,
            Boolean lotControlled,
            BigDecimal currentStock,
            SuggestedAction suggestedAction,
            String targetMaterialId,
            String targetMaterialName,
            String matchReason,
            List<SimilarMaterial> similarMaterials,
            List<String> warnings,
            List<String> errors
    ) {}

    public record ImportPreviewResponse(
            int totalRows,
            int createCount,
            int updateCount,
            int reviewCount,
            int unchangedCount,
            int errorCount,
            List<ImportPreviewRow> rows
    ) {}

    public record ImportDecision(
            @Min(2) int rowNumber,
            @NotNull ImportAction action,
            String targetMaterialId
    ) {}

    public record ImportConfirmRequest(
            @NotEmpty List<@Valid ImportDecision> decisions
    ) {}

    public record ImportResult(
            int created,
            int updated,
            int skipped
    ) {}
}
