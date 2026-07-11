package br.com.clinicaleve.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByClinicIdAndEmailIgnoreCaseAndActiveTrue(String clinicId, String email);

    Optional<AppUser> findByIdAndClinicId(String id, String clinicId);

    List<AppUser> findByClinicIdOrderByActiveDescNameAsc(String clinicId);

    boolean existsByClinicIdAndEmailIgnoreCase(String clinicId, String email);

    boolean existsByClinicIdAndEmailIgnoreCaseAndIdNot(String clinicId, String email, String id);

    long countByClinicIdAndRoleAndActiveTrue(String clinicId, Role role);

    boolean existsByClinicId(String clinicId);
}
