package br.com.clinicaleve.finance;

import br.com.clinicaleve.finance.FinanceDtos.*;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialCategoryService {
    private final FinancialCategoryRepository repository;

    @Transactional
    public List<CategoryResponse> list() {
        var clinicId = TenantAccess.currentClinicId();
        var items = repository.findByClinicIdAndActiveTrueOrderByTypeAscNameAsc(clinicId);
        if (items.isEmpty()) {
            repository.saveAll(List.of(
                    category(clinicId, "Consultas", FinancialEntryType.INCOME),
                    category(clinicId, "Procedimentos", FinancialEntryType.INCOME),
                    category(clinicId, "Convênios", FinancialEntryType.INCOME),
                    category(clinicId, "Outros recebimentos", FinancialEntryType.INCOME),
                    category(clinicId, "Folha de pagamento", FinancialEntryType.EXPENSE),
                    category(clinicId, "Fornecedores", FinancialEntryType.EXPENSE),
                    category(clinicId, "Aluguel e estrutura", FinancialEntryType.EXPENSE),
                    category(clinicId, "Impostos", FinancialEntryType.EXPENSE),
                    category(clinicId, "Materiais", FinancialEntryType.EXPENSE),
                    category(clinicId, "Outros pagamentos", FinancialEntryType.EXPENSE)
            ));
            items = repository.findByClinicIdAndActiveTrueOrderByTypeAscNameAsc(clinicId);
        }
        return items.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var name = request.name().trim();
        if (repository.existsByClinicIdAndTypeAndNameIgnoreCase(clinicId, request.type(), name)) {
            throw new IllegalArgumentException("Já existe uma categoria financeira com este nome");
        }
        return CategoryResponse.from(repository.save(category(clinicId, name, request.type())));
    }

    private FinancialCategory category(String clinicId, String name, FinancialEntryType type) {
        var c = new FinancialCategory(); c.setClinicId(clinicId); c.setName(name); c.setType(type); return c;
    }
}
