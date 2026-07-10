package br.com.clinicaleve.professional;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfessionalRepository extends JpaRepository<Professional, String> {

    List<Professional> findByClinicIdAndActiveTrueOrderByName(String clinicId);

    Optional<Professional> findByIdAndClinicId(String id, String clinicId);
}
