package br.com.clinicaleve.clinical;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicalEncounterRepository extends JpaRepository<ClinicalEncounter, String> {

    Optional<ClinicalEncounter> findByIdAndClinicId(String id, String clinicId);

    Optional<ClinicalEncounter> findByAppointmentIdAndClinicId(String appointmentId, String clinicId);

    List<ClinicalEncounter> findByClinicIdOrderByCreatedAtDesc(String clinicId);

    List<ClinicalEncounter> findByClinicIdAndPatientIdOrderByCreatedAtDesc(String clinicId, String patientId);

    List<ClinicalEncounter> findByClinicIdAndProfessionalIdOrderByCreatedAtDesc(String clinicId, String professionalId);
}
