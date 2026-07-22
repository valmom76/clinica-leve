package br.com.clinicaleve.clinical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClinicalEncounterVersionRepository extends JpaRepository<ClinicalEncounterVersion, String> {

    long countByClinicIdAndEncounterId(String clinicId, String encounterId);

    List<ClinicalEncounterVersion> findByClinicIdAndEncounterIdOrderByVersionNumberDesc(
            String clinicId,
            String encounterId
    );
}
