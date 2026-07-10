package br.com.clinicaleve.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {

    List<Appointment> findByClinicIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAt(
            String clinicId,
            Instant from,
            Instant to
    );

    @Query("""
            select (count(a) > 0)
            from Appointment a
            where a.clinicId = :clinicId
              and a.professionalId = :professionalId
              and a.status not in (
                  br.com.clinicaleve.appointment.AppointmentStatus.CANCELLED,
                  br.com.clinicaleve.appointment.AppointmentStatus.NO_SHOW
              )
              and a.startAt < :endAt
              and a.endAt > :startAt
            """)
    boolean hasConflict(
            @Param("clinicId") String clinicId,
            @Param("professionalId") String professionalId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
