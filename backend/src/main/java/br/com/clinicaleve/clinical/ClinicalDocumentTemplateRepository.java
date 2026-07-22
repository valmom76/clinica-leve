package br.com.clinicaleve.clinical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalDocumentTemplateRepository extends JpaRepository<ClinicalDocumentTemplate, String> {

    Optional<ClinicalDocumentTemplate> findByIdAndClinicId(String id, String clinicId);

    List<ClinicalDocumentTemplate> findByClinicIdOrderByFavoriteDescNameAsc(String clinicId);

    boolean existsByClinicIdAndNameIgnoreCaseAndIdNot(String clinicId, String name, String id);
}
