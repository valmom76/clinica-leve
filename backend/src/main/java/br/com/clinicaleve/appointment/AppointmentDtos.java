package br.com.clinicaleve.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record AppointmentRequest(
            @NotBlank String patientId,
            @NotBlank String professionalId,
            @NotBlank String specialtyId,
            @NotNull Instant startAt,
            @NotNull Instant endAt,
            AppointmentStatus status,
            @Size(max = 1000) String notes
    ) {
    }

    public record AppointmentResponse(
            String id,
            String patientId,
            String patientName,
            String professionalId,
            String professionalName,
            String specialtyId,
            String specialtyName,
            String color,
            Instant startAt,
            Instant endAt,
            AppointmentStatus status,
            String notes
    ) {
    }
}
