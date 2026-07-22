package br.com.clinicaleve.auth;

import br.com.clinicaleve.auth.AuthDtos.ClinicSummary;
import br.com.clinicaleve.auth.AuthDtos.LoginRequest;
import br.com.clinicaleve.auth.AuthDtos.LoginResponse;
import br.com.clinicaleve.auth.AuthDtos.UserSummary;
import br.com.clinicaleve.tenant.ClinicRepository;
import br.com.clinicaleve.tenant.ClinicBrandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClinicRepository clinicRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        var clinic = clinicRepository.findBySlugAndActiveTrue(request.clinicSlug().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        var user = userRepository
                .findByClinicIdAndEmailIgnoreCaseAndActiveTrue(clinic.getId(), request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        var token = jwtService.create(user, clinic);
        return new LoginResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                new UserSummary(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getProfessionalId()
                ),
                new ClinicSummary(
                        clinic.getId(),
                        clinic.getName(),
                        clinic.getSlug(),
                        clinic.getTimezone(),
                        ClinicBrandingService.logoUrl(clinic),
                        clinic.getThemeKey()
                )
        );
    }
}
