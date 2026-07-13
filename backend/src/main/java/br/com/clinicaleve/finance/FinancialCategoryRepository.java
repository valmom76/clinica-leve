package br.com.clinicaleve.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, String> {
    List<FinancialCategory> findByClinicIdAndActiveTrueOrderByTypeAscNameAsc(String clinicId);
    Optional<FinancialCategory> findByIdAndClinicIdAndActiveTrue(String id, String clinicId);
    boolean existsByClinicIdAndTypeAndNameIgnoreCase(String clinicId, FinancialEntryType type, String name);
}
