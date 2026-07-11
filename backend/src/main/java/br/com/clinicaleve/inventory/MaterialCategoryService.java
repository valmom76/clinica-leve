package br.com.clinicaleve.inventory;

import br.com.clinicaleve.inventory.InventoryDtos.CategoryRequest;
import br.com.clinicaleve.inventory.InventoryDtos.CategoryResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialCategoryService {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "Insumos clínicos",
            "Material hospitalar",
            "Material de expediente",
            "Material de limpeza",
            "Odontologia",
            "Outros"
    );

    private final MaterialCategoryRepository repository;

    @Transactional
    public List<CategoryResponse> list() {
        var clinicId = TenantAccess.currentClinicId();
        var categories = repository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        if (categories.isEmpty()) {
            categories = DEFAULT_CATEGORIES.stream()
                    .map(name -> category(clinicId, name))
                    .toList();
            repository.saveAll(categories);
            categories = repository.findByClinicIdAndActiveTrueOrderByName(clinicId);
        }
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var name = request.name().trim();
        if (repository.existsByClinicIdAndNameIgnoreCase(clinicId, name)) {
            throw new IllegalArgumentException("Já existe uma categoria com este nome");
        }
        return CategoryResponse.from(repository.save(category(clinicId, name)));
    }

    private MaterialCategory category(String clinicId, String name) {
        var category = new MaterialCategory();
        category.setClinicId(clinicId);
        category.setName(name);
        return category;
    }
}
