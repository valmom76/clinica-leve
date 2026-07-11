package br.com.clinicaleve.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, String> {

    List<MaterialCategory> findByClinicIdAndActiveTrueOrderByName(String clinicId);

    Optional<MaterialCategory> findByIdAndClinicIdAndActiveTrue(String id, String clinicId);

    boolean existsByClinicIdAndNameIgnoreCase(String clinicId, String name);
}
