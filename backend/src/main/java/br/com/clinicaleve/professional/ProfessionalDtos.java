package br.com.clinicaleve.professional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ProfessionalDtos {

    private ProfessionalDtos() {
    }

    public record ProfessionalRequest(
            @NotBlank String specialtyId,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String council,
            @Email @Size(max = 190) String email,
            @Size(max = 30) String phone
    ) {
    }

    public record ProfessionalResponse(
            String id,
            String name,
            String council,
            String email,
            String phone,
            String specialtyId,
            String specialtyName,
            String specialtyColor,
            boolean active
    ) {
    }
}
