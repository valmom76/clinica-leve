package br.com.clinicaleve.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, String> {

    List<Material> findByClinicIdAndActiveTrueOrderByName(String clinicId);

    Optional<Material> findByIdAndClinicIdAndActiveTrue(String id, String clinicId);

    boolean existsByClinicIdAndSkuIgnoreCase(String clinicId, String sku);

    boolean existsByClinicIdAndSkuIgnoreCaseAndIdNot(String clinicId, String sku, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select material from Material material
            where material.id = :id
              and material.clinicId = :clinicId
              and material.active = true
            """)
    Optional<Material> findActiveForUpdate(
            @Param("id") String id,
            @Param("clinicId") String clinicId
    );
}
