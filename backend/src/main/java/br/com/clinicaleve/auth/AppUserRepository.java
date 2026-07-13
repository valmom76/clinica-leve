package br.com.clinicaleve.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    Optional<AppUser> findByClinicIdAndEmailIgnoreCaseAndActiveTrue(String clinicId, String email);

    Optional<AppUser> findByIdAndClinicId(String id, String clinicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id = :id and u.clinicId = :clinicId")
    Optional<AppUser> findForUpdate(@Param("id") String id, @Param("clinicId") String clinicId);

    List<AppUser> findByClinicIdOrderByActiveDescNameAsc(String clinicId);

    List<AppUser> findByClinicIdAndActiveTrueOrderByNameAsc(String clinicId);

    boolean existsByClinicIdAndEmailIgnoreCase(String clinicId, String email);

    boolean existsByClinicIdAndEmailIgnoreCaseAndIdNot(String clinicId, String email, String id);

    long countByClinicIdAndRoleAndActiveTrue(String clinicId, Role role);

    boolean existsByClinicId(String clinicId);
}
