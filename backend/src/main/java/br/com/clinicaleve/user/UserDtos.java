package br.com.clinicaleve.user;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull Role role,
            String professionalId,
            @Min(60) @Max(720) Integer expectedDailyMinutes
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @Size(min = 8, max = 72) String password,
            @NotNull Role role,
            String professionalId,
            @Min(60) @Max(720) Integer expectedDailyMinutes,
            boolean active
    ) {
    }

    public record UserResponse(
            String id,
            String name,
            String email,
            Role role,
            String professionalId,
            int expectedDailyMinutes,
            boolean active
    ) {
        static UserResponse from(AppUser user) {
            return new UserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getProfessionalId(),
                    user.getExpectedDailyMinutes(),
                    user.isActive()
            );
        }
    }
}
