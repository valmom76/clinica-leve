package br.com.clinicaleve.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MaterialBatchRepository extends JpaRepository<MaterialBatch, String> {

    Optional<MaterialBatch> findByClinicIdAndMaterialIdAndLotNumberIgnoreCase(
            String clinicId,
            String materialId,
            String lotNumber
    );

    List<MaterialBatch> findByClinicIdAndCurrentQuantityGreaterThan(
            String clinicId,
            BigDecimal quantity
    );

    List<MaterialBatch> findByClinicIdAndMaterialIdAndCurrentQuantityGreaterThan(
            String clinicId,
            String materialId,
            BigDecimal quantity
    );

    List<MaterialBatch> findByClinicIdAndIdIn(String clinicId, Set<String> ids);
}
