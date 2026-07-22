package br.com.clinicaleve.appointment.messaging;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentMessageRepository extends JpaRepository<AppointmentMessage, String> {

    List<AppointmentMessage> findByAppointmentIdAndClinicIdOrderByCreatedAtDesc(
            String appointmentId,
            String clinicId
    );

    Optional<AppointmentMessage> findByIdAndClinicId(String id, String clinicId);

    Optional<AppointmentMessage> findByProviderMessageId(String providerMessageId);

    boolean existsByResponseProviderMessageId(String responseProviderMessageId);

    @Query("""
            select m.id from AppointmentMessage m
            where m.status in (
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.PENDING,
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.FAILED
            )
              and m.nextAttemptAt <= :now
            order by m.nextAttemptAt
            """)
    List<String> findDueIds(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from AppointmentMessage m where m.id = :id")
    Optional<AppointmentMessage> findForProcessing(@Param("id") String id);

    @Query("""
            select m from AppointmentMessage m
            where m.appointmentId = :appointmentId
              and m.clinicId = :clinicId
              and m.status in (
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.PENDING,
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.FAILED,
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.PROCESSING
              )
            """)
    List<AppointmentMessage> findCancellable(
            @Param("appointmentId") String appointmentId,
            @Param("clinicId") String clinicId
    );

    @Query("""
            select m from AppointmentMessage m
            where m.appointmentId = :appointmentId
              and m.clinicId = :clinicId
              and m.purpose = br.com.clinicaleve.appointment.messaging.MessagePurpose.CONFIRMATION
              and m.status in (
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.PENDING,
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.FAILED,
                br.com.clinicaleve.appointment.messaging.AppointmentMessageStatus.PROCESSING
              )
            """)
    List<AppointmentMessage> findCancellableConfirmations(
            @Param("appointmentId") String appointmentId,
            @Param("clinicId") String clinicId
    );
}
