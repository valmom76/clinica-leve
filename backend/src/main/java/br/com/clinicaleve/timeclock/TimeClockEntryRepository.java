package br.com.clinicaleve.timeclock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeClockEntryRepository extends JpaRepository<TimeClockEntry, String> {

    @Query("""
            select e from TimeClockEntry e
            where e.clinicId = :clinicId and e.userId = :userId
              and e.occurredAt >= :from and e.occurredAt < :to
            order by e.occurredAt asc, e.createdAt asc
            """)
    List<TimeClockEntry> findDayForUser(
            @Param("clinicId") String clinicId,
            @Param("userId") String userId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Query("""
            select e from TimeClockEntry e
            where e.clinicId = :clinicId
              and e.occurredAt >= :from and e.occurredAt < :to
            order by e.userId asc, e.occurredAt asc, e.createdAt asc
            """)
    List<TimeClockEntry> findDayForClinic(
            @Param("clinicId") String clinicId,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from TimeClockEntry e where e.id = :id and e.clinicId = :clinicId")
    Optional<TimeClockEntry> findForUpdate(
            @Param("id") String id,
            @Param("clinicId") String clinicId
    );
}
