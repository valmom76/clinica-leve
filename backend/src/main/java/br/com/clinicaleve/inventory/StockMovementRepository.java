package br.com.clinicaleve.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    List<StockMovement> findTop100ByClinicIdAndMaterialIdOrderByOccurredAtDesc(
            String clinicId,
            String materialId
    );

    @Query("""
            select movement from StockMovement movement
            where movement.clinicId = :clinicId
              and movement.occurredAt >= :from and movement.occurredAt < :to
              and (:materialId is null or movement.materialId = :materialId)
              and (:type is null or movement.type = :type)
            order by movement.occurredAt desc
            """)
    List<StockMovement> findForReport(
            @Param("clinicId") String clinicId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("materialId") String materialId,
            @Param("type") StockMovementType type
    );
}
