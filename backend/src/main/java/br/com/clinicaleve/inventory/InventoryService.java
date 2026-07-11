package br.com.clinicaleve.inventory;

import br.com.clinicaleve.inventory.InventoryDtos.MaterialRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MaterialResponse;
import br.com.clinicaleve.inventory.InventoryDtos.MovementRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MovementResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MaterialRepository materialRepository;
    private final MaterialCategoryRepository categoryRepository;
    private final MaterialBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;

    @Transactional(readOnly = true)
    public List<MaterialResponse> list(String search, boolean lowStockOnly) {
        var clinicId = TenantAccess.currentClinicId();
        var categories = categoryRepository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                .stream()
                .collect(Collectors.toMap(MaterialCategory::getId, MaterialCategory::getName));
        var nearestExpirations = batchRepository
                .findByClinicIdAndCurrentQuantityGreaterThan(clinicId, ZERO)
                .stream()
                .filter(batch -> batch.getExpirationDate() != null)
                .collect(Collectors.toMap(
                        MaterialBatch::getMaterialId,
                        MaterialBatch::getExpirationDate,
                        (first, second) -> first.isBefore(second) ? first : second
                ));
        var normalizedSearch = search == null ? "" : search.trim().toLowerCase();

        return materialRepository.findByClinicIdAndActiveTrueOrderByName(clinicId)
                .stream()
                .filter(material -> !lowStockOnly
                        || material.getMinimumStock().signum() > 0
                        && material.getCurrentStock().compareTo(material.getMinimumStock()) <= 0)
                .filter(material -> normalizedSearch.isBlank()
                        || material.getName().toLowerCase().contains(normalizedSearch)
                        || value(material.getSku()).contains(normalizedSearch)
                        || value(categories.get(material.getCategoryId())).contains(normalizedSearch))
                .map(material -> MaterialResponse.from(
                        material,
                        categories.getOrDefault(material.getCategoryId(), "Sem categoria"),
                        nearestExpirations.get(material.getId())
                ))
                .toList();
    }

    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var category = categoryRepository
                .findByIdAndClinicIdAndActiveTrue(request.categoryId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        var sku = blankToNull(request.sku());
        if (sku != null && materialRepository.existsByClinicIdAndSkuIgnoreCase(clinicId, sku)) {
            throw new IllegalArgumentException("Já existe um material com este código");
        }

        var material = new Material();
        material.setClinicId(clinicId);
        material.setCategoryId(category.getId());
        material.setName(request.name().trim());
        material.setSku(sku);
        material.setUnit(request.unit().trim());
        material.setMinimumStock(request.minimumStock());
        material.setCurrentStock(ZERO);
        material.setLotControlled(request.lotControlled());
        return MaterialResponse.from(
                materialRepository.save(material),
                category.getName(),
                null
        );
    }

    @Transactional
    public MaterialResponse update(String id, MaterialRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var material = materialRepository.findActiveForUpdate(id, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        var category = categoryRepository
                .findByIdAndClinicIdAndActiveTrue(request.categoryId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada"));
        var sku = blankToNull(request.sku());
        if (sku != null
                && materialRepository.existsByClinicIdAndSkuIgnoreCaseAndIdNot(clinicId, sku, id)) {
            throw new IllegalArgumentException("Já existe um material com este código");
        }
        if (material.isLotControlled() != request.lotControlled()
                && material.getCurrentStock().signum() > 0) {
            throw new IllegalStateException(
                    "O controle por lote só pode ser alterado quando o saldo estiver zerado"
            );
        }

        material.setCategoryId(category.getId());
        material.setName(request.name().trim());
        material.setSku(sku);
        material.setUnit(request.unit().trim());
        material.setMinimumStock(request.minimumStock());
        material.setLotControlled(request.lotControlled());
        return MaterialResponse.from(
                materialRepository.save(material),
                category.getName(),
                responseFor(material).nearestExpiration()
        );
    }

    @Transactional
    public MaterialResponse move(String materialId, MovementRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var material = materialRepository.findActiveForUpdate(materialId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));

        if (request.type() == StockMovementType.ENTRY) {
            registerEntry(material, request);
        } else {
            registerExit(material, request);
        }
        materialRepository.save(material);
        return responseFor(material);
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> movements(String materialId) {
        var clinicId = TenantAccess.currentClinicId();
        materialRepository.findByIdAndClinicIdAndActiveTrue(materialId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Material não encontrado"));
        var movements = movementRepository
                .findTop100ByClinicIdAndMaterialIdOrderByOccurredAtDesc(clinicId, materialId);
        var batchIds = movements.stream()
                .map(StockMovement::getBatchId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<String, MaterialBatch> batches = batchRepository.findAllById(batchIds)
                .stream()
                .filter(batch -> clinicId.equals(batch.getClinicId()))
                .collect(Collectors.toMap(MaterialBatch::getId, Function.identity()));
        return movements.stream()
                .map(movement -> MovementResponse.from(
                        movement,
                        batches.get(movement.getBatchId())
                ))
                .toList();
    }

    private void registerEntry(Material material, MovementRequest request) {
        MaterialBatch batch = null;
        if (material.isLotControlled()) {
            var lotNumber = blankToNull(request.lotNumber());
            if (lotNumber == null) {
                throw new IllegalArgumentException("Informe o lote para este material");
            }
            batch = batchRepository
                    .findByClinicIdAndMaterialIdAndLotNumberIgnoreCase(
                            material.getClinicId(),
                            material.getId(),
                            lotNumber
                    )
                    .orElseGet(() -> newBatch(material, lotNumber, request.expirationDate()));
            if (batch.getExpirationDate() != null
                    && request.expirationDate() != null
                    && !batch.getExpirationDate().equals(request.expirationDate())) {
                throw new IllegalArgumentException("O lote já está cadastrado com outra validade");
            }
            if (batch.getExpirationDate() == null) {
                batch.setExpirationDate(request.expirationDate());
            }
            batch.setCurrentQuantity(batch.getCurrentQuantity().add(request.quantity()));
            batch = batchRepository.save(batch);
        }

        var balance = material.getCurrentStock().add(request.quantity());
        material.setCurrentStock(balance);
        movementRepository.save(movement(
                material,
                batch,
                StockMovementType.ENTRY,
                request.quantity(),
                balance,
                request.reason()
        ));
    }

    private void registerExit(Material material, MovementRequest request) {
        if (material.getCurrentStock().compareTo(request.quantity()) < 0) {
            throw new IllegalStateException("Estoque insuficiente para esta saída");
        }

        var balance = material.getCurrentStock();
        if (!material.isLotControlled()) {
            balance = balance.subtract(request.quantity());
            movementRepository.save(movement(
                    material,
                    null,
                    StockMovementType.EXIT,
                    request.quantity(),
                    balance,
                    request.reason()
            ));
        } else {
            var remaining = request.quantity();
            var batches = batchRepository
                    .findByClinicIdAndMaterialIdAndCurrentQuantityGreaterThan(
                            material.getClinicId(),
                            material.getId(),
                            ZERO
                    )
                    .stream()
                    .sorted(batchComparator())
                    .toList();
            for (var batch : batches) {
                if (remaining.signum() == 0) {
                    break;
                }
                var consumed = remaining.min(batch.getCurrentQuantity());
                batch.setCurrentQuantity(batch.getCurrentQuantity().subtract(consumed));
                batchRepository.save(batch);
                remaining = remaining.subtract(consumed);
                balance = balance.subtract(consumed);
                movementRepository.save(movement(
                        material,
                        batch,
                        StockMovementType.EXIT,
                        consumed,
                        balance,
                        request.reason()
                ));
            }
            if (remaining.signum() > 0) {
                throw new IllegalStateException("Os lotes disponíveis não cobrem esta saída");
            }
        }
        material.setCurrentStock(balance);
    }

    private StockMovement movement(
            Material material,
            MaterialBatch batch,
            StockMovementType type,
            BigDecimal quantity,
            BigDecimal balanceAfter,
            String reason
    ) {
        var movement = new StockMovement();
        movement.setClinicId(material.getClinicId());
        movement.setMaterialId(material.getId());
        movement.setBatchId(batch == null ? null : batch.getId());
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setBalanceAfter(balanceAfter);
        movement.setReason(reason.trim());
        movement.setCreatedByUserId(TenantAccess.currentUserId());
        movement.setOccurredAt(Instant.now());
        return movement;
    }

    private MaterialBatch newBatch(Material material, String lotNumber, LocalDate expirationDate) {
        var batch = new MaterialBatch();
        batch.setClinicId(material.getClinicId());
        batch.setMaterialId(material.getId());
        batch.setLotNumber(lotNumber);
        batch.setExpirationDate(expirationDate);
        return batch;
    }

    private MaterialResponse responseFor(Material material) {
        var categoryName = categoryRepository
                .findByIdAndClinicIdAndActiveTrue(material.getCategoryId(), material.getClinicId())
                .map(MaterialCategory::getName)
                .orElse("Sem categoria");
        var nearestExpiration = batchRepository
                .findByClinicIdAndMaterialIdAndCurrentQuantityGreaterThan(
                        material.getClinicId(),
                        material.getId(),
                        ZERO
                )
                .stream()
                .map(MaterialBatch::getExpirationDate)
                .filter(date -> date != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        return MaterialResponse.from(material, categoryName, nearestExpiration);
    }

    private Comparator<MaterialBatch> batchComparator() {
        return Comparator
                .comparing(
                        MaterialBatch::getExpirationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
                .thenComparing(MaterialBatch::getCreatedAt);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String value(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
