package br.com.clinicaleve.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClinicRepository extends JpaRepository<Clinic, String> {

    Optional<Clinic> findBySlugAndActiveTrue(String slug);
}
