package br.com.clinicaleve.auth;

import br.com.clinicaleve.tenant.ClinicTheme;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public record AuthCapabilitiesResponse(
            boolean passwordRecoveryEnabled,
            int minimumPasswordLength
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank String clinicSlug,
            @Email @NotBlank String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 10, max = 72) String newPassword
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 10, max = 72) String newPassword
    ) {
    }

    public record UserSummary(
            String id,
            String name,
            String email,
            Role role,
            String professionalId
    ) {
    }

    public record ClinicSummary(
            String id,
            String name,
            String slug,
            String timezone,
            String logoUrl,
            ClinicTheme themeKey
    ) {
    }
}
