package br.com.clinicaleve.specialty;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpecialtyRepository extends JpaRepository<Specialty, String> {

    List<Specialty> findByClinicIdAndActiveTrueOrderByName(String clinicId);

    Optional<Specialty> findByIdAndClinicId(String id, String clinicId);

    boolean existsByClinicIdAndNameIgnoreCase(String clinicId, String name);
}
