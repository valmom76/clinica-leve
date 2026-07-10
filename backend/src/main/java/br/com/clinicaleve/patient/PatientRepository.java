package br.com.clinicaleve.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, String> {

    List<Patient> findByClinicIdAndActiveTrueOrderByName(String clinicId);

    List<Patient> findByClinicIdAndActiveTrueAndNameContainingIgnoreCaseOrderByName(
            String clinicId,
            String name
    );

    Optional<Patient> findByIdAndClinicId(String id, String clinicId);
}
