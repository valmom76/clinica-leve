package br.com.clinicaleve.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank String clinicSlug,
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            UserSummary user,
            ClinicSummary clinic
    ) {
    }

    public record UserSummary(String id, String name, String email, Role role) {
    }

    public record ClinicSummary(String id, String name, String slug, String timezone) {
    }
}
