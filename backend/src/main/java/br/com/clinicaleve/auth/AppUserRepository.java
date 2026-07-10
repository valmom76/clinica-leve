package br.com.clinicaleve.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByClinicIdAndEmailIgnoreCaseAndActiveTrue(String clinicId, String email);

    boolean existsByClinicId(String clinicId);
}
