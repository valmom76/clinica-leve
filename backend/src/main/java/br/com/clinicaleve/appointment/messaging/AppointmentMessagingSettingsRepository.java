package br.com.clinicaleve.appointment.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentMessagingSettingsRepository
        extends JpaRepository<AppointmentMessagingSettings, String> {

    Optional<AppointmentMessagingSettings> findByClinicId(String clinicId);
}
