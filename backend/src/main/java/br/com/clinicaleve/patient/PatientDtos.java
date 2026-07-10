package br.com.clinicaleve.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 14) String cpf,
            LocalDate birthDate,
            @Email @Size(max = 190) String email,
            @NotBlank @Size(max = 30) String phone
    ) {
    }

    public record PatientResponse(
            String id,
            String name,
            String cpf,
            LocalDate birthDate,
            String email,
            String phone,
            boolean active
    ) {
        public static PatientResponse from(Patient patient) {
            return new PatientResponse(
                    patient.getId(),
                    patient.getName(),
                    patient.getCpf(),
                    patient.getBirthDate(),
                    patient.getEmail(),
                    patient.getPhone(),
                    patient.isActive()
            );
        }
    }
}
