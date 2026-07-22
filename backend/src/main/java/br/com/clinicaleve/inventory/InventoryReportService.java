package br.com.clinicaleve.inventory;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.inventory.InventoryReportDtos.MovementReportResponse;
import br.com.clinicaleve.inventory.InventoryReportDtos.MovementReportRow;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryReportService {

    private final StockMovementRepository movementRepository;
    private final MaterialRepository materialRepository;
    private final MaterialBatchRepository batchRepository;
    private final AppUserRepository userRepository;
    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public MovementReportResponse movements(
            LocalDate from,
            LocalDate to,
            String materialId,
            StockMovementType type
    ) {
        validatePeriod(from, to);
        var clinicId = TenantAccess.currentClinicId();
        var zone = clinicZone(clinicId);
        var normalizedMaterialId = materialId == null || materialId.isBlank() ? null : materialId;
        Material selectedMaterial = null;
        if (normalizedMaterialId != null) {
            selectedMaterial = materialRepository.findByIdAndClinicIdAndActiveTrue(normalizedMaterialId, clinicId)
                    .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        }

        var rows = movementRepository.findForReport(
                clinicId,
                from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant(),
                normalizedMaterialId,
                type
        );
        var materials = materialRepository.findByClinicIdAndActiveTrueOrderByName(clinicId).stream()
                .collect(Collectors.toMap(Material::getId, Function.identity()));
        var users = userRepository.findByClinicIdOrderByActiveDescNameAsc(clinicId).stream()
                .collect(Collectors.toMap(AppUser::getId, AppUser::getName));
        var batchIds = rows.stream().map(StockMovement::getBatchId).filter(id -> id != null).collect(Collectors.toSet());
        var batches = batchIds.isEmpty()
                ? Collections.<String, MaterialBatch>emptyMap()
                : batchRepository.findByClinicIdAndIdIn(clinicId, batchIds).stream()
                .collect(Collectors.toMap(MaterialBatch::getId, Function.identity()));

        var responseRows = rows.stream().map(row -> {
            var material = materials.get(row.getMaterialId());
            var batch = row.getBatchId() == null ? null : batches.get(row.getBatchId());
            return new MovementReportRow(
                    row.getId(),
                    row.getMaterialId(),
                    material == null ? "Material inativo" : material.getName(),
                    material == null ? "un" : material.getUnit(),
                    row.getType(),
                    row.getQuantity(),
                    row.getBalanceAfter(),
                    row.getReason(),
                    batch == null ? null : batch.getLotNumber(),
                    users.getOrDefault(row.getCreatedByUserId(), "Usuário inativo"),
                    LocalDateTime.ofInstant(row.getOccurredAt(), zone)
            );
        }).toList();

        var entryCount = (int) rows.stream().filter(row -> row.getType() == StockMovementType.ENTRY).count();
        var exitCount = (int) rows.stream().filter(row -> row.getType() == StockMovementType.EXIT).count();
        var totalEntries = normalizedMaterialId == null ? null : quantity(rows, StockMovementType.ENTRY);
        var totalExits = normalizedMaterialId == null ? null : quantity(rows, StockMovementType.EXIT);
        return new MovementReportResponse(
                from,
                to,
                normalizedMaterialId,
                selectedMaterial == null ? null : selectedMaterial.getName(),
                selectedMaterial == null ? null : selectedMaterial.getUnit(),
                rows.size(),
                entryCount,
                exitCount,
                (int) rows.stream().map(StockMovement::getMaterialId).distinct().count(),
                totalEntries,
                totalExits,
                responseRows
        );
    }

    private BigDecimal quantity(java.util.List<StockMovement> rows, StockMovementType type) {
        return rows.stream().filter(row -> row.getType() == type)
                .map(StockMovement::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Informe o período do relatório");
        if (from.isAfter(to)) throw new IllegalArgumentException("A data inicial deve ser anterior à data final");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new IllegalArgumentException("O relatório pode abranger no máximo 366 dias");
        }
    }

    private ZoneId clinicZone(String clinicId) {
        var timezone = clinicRepository.findById(clinicId)
                .map(clinic -> clinic.getTimezone())
                .orElse("America/Fortaleza");
        return ZoneId.of(timezone);
    }
}
