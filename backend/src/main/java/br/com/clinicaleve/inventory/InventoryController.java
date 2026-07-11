package br.com.clinicaleve.inventory;

import br.com.clinicaleve.inventory.InventoryDtos.CategoryRequest;
import br.com.clinicaleve.inventory.InventoryDtos.CategoryResponse;
import br.com.clinicaleve.inventory.InventoryDtos.MaterialRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MaterialResponse;
import br.com.clinicaleve.inventory.InventoryDtos.MovementRequest;
import br.com.clinicaleve.inventory.InventoryDtos.MovementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOCK')")
public class InventoryController {

    private final MaterialCategoryService categoryService;
    private final InventoryService inventoryService;

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
}
