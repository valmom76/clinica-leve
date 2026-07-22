package br.com.clinicaleve.clinical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalDocumentRepository extends JpaRepository<ClinicalDocument, String> {

    Optional<ClinicalDocument> findByIdAndClinicId(String id, String clinicId);

    Optional<ClinicalDocument> findByVerificationCode(String verificationCode);

    List<ClinicalDocument> findByClinicIdAndEncounterIdOrderByCreatedAtDesc(String clinicId, String encounterId);
}
