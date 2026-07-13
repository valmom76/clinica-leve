package br.com.clinicaleve.finance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface FinancialEntryRepository extends JpaRepository<FinancialEntry, String> {
    List<FinancialEntry> findByClinicIdOrderByDueDateDescCreatedAtDesc(String clinicId);
    @Query("""
            select e from FinancialEntry e
            where e.clinicId = :clinicId
              and (
                (e.dueDate >= :from and e.dueDate <= :to)
                or (e.paymentDate is not null and e.paymentDate >= :from and e.paymentDate <= :to)
              )
            """)
    List<FinancialEntry> findForReport(
            @Param("clinicId") String clinicId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from FinancialEntry e where e.id = :id and e.clinicId = :clinicId")
    Optional<FinancialEntry> findForUpdate(@Param("id") String id, @Param("clinicId") String clinicId);
}
