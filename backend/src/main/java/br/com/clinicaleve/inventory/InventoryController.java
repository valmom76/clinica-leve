package br.com.clinicaleve.inventory;

import br.com.clinicaleve.inventory.InventoryDtos.CategoryRequest;
import br.com.clinicaleve.inventory.InventoryDtos.CategoryResponse;
import br.com.clinicaleve.inventory.InventoryDtos.MaterialRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MaterialResponse;
import br.com.clinicaleve.inventory.InventoryDtos.MovementRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MovementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import br.com.clinicaleve.inventory.InventoryReportDtos.MovementReportResponse;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportConfirmRequest;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportPreviewResponse;
import br.com.clinicaleve.inventory.InventoryImportDtos.ImportResult;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOCK')")
public class InventoryController {

    private final MaterialCategoryService categoryService;
    private final InventoryService inventoryService;
    private final InventoryReportService reportService;
    private final InventoryImportService importService;

    @GetMapping("/categories")
    List<CategoryResponse> categories() {
        return categoryService.list();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping("/materials")
    List<MaterialResponse> materials(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean lowStock
    ) {
        return inventoryService.list(search, lowStock);
    }

    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    MaterialResponse createMaterial(@Valid @RequestBody MaterialRequest request) {
        return inventoryService.create(request);
    }

    @PutMapping("/materials/{id}")
    MaterialResponse updateMaterial(
            @PathVariable String id,
            @Valid @RequestBody MaterialRequest request
    ) {
        return inventoryService.update(id, request);
    }

    @PostMapping("/materials/{id}/movements")
    MaterialResponse move(
            @PathVariable String id,
            @Valid @RequestBody MovementRequest request
    ) {
        return inventoryService.move(id, request);
    }

    @GetMapping("/materials/{id}/movements")
    List<MovementResponse> movements(@PathVariable String id) {
        return inventoryService.movements(id);
    }

    @GetMapping("/reports/movements")
    MovementReportResponse movementReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String materialId,
            @RequestParam(required = false) StockMovementType type
    ) {
        return reportService.movements(from, to, materialId, type);
    }

    @GetMapping(value = "/materials/import-template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ResponseEntity<byte[]> importTemplate() {
        var disposition = ContentDisposition.attachment()
                .filename("modelo-importacao-materiais.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(importService.template());
    }

    @PostMapping(value = "/materials/import-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportPreviewResponse importPreview(@RequestPart("file") MultipartFile file) {
        return importService.preview(file);
    }

    @PostMapping(value = "/materials/import-confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ImportResult importConfirm(
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("decisions") ImportConfirmRequest request
    ) {
        return importService.confirm(file, request);
    }
}
