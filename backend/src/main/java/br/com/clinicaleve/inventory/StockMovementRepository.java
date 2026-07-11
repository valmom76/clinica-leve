package br.com.clinicaleve.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    List<StockMovement> findTop100ByClinicIdAndMaterialIdOrderByOccurredAtDesc(
            String clinicId,
            String materialId
    );
}
