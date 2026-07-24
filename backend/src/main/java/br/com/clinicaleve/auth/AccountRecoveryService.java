package br.com.clinicaleve.auth;

import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountRecoveryService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClinicRepository clinicRepository;
    private final AppUserRepository userRepository;
    private final AccountActionTokenRepository tokenRepository;
    private final AccountMailService mailService;
    private final AccountMailProperties mailProperties;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    @Transactional
    public void requestPasswordReset(AuthDtos.ForgotPasswordRequest request, String requestedIp) {
        if (!mailService.isEnabled()) {
            return;
        }
        var clinic = clinicRepository.findBySlugAndActiveTrue(request.clinicSlug().trim().toLowerCase())
                .orElse(null);
        if (clinic == null) {
            return;
        }
        var user = userRepository.findByClinicIdAndEmailIgnoreCaseAndActiveTrue(
                clinic.getId(),
                request.email().trim().toLowerCase()
        ).orElse(null);
        if (user == null) {
            return;
        }
        var recent = tokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(
                user.getId(),
                AccountActionPurpose.PASSWORD_RESET,
                Instant.now().minus(Duration.ofHours(1))
        );
        if (recent >= 3) {
            return;
        }
        var rawToken = issue(
                user,
                AccountActionPurpose.PASSWORD_RESET,
                Duration.ofMinutes(Math.max(10, mailProperties.resetExpirationMinutes())),
                requestedIp
        );
        try {
            mailService.sendPasswordReset(user, clinic, rawToken);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao enviar recuperação de senha para o usuário {}", user.getId(), exception);
        }
    }

    @Transactional
    public void sendInvitation(AppUser user, String requestedIp) {
        if (!mailService.isEnabled()) {
            throw new IllegalStateException("Configure o envio de e-mail antes de usar convites");
        }
        var clinic = clinicRepository.findById(user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));
        var rawToken = issue(
                user,
                AccountActionPurpose.INVITATION,
                Duration.ofHours(Math.max(1, mailProperties.invitationExpirationHours())),
                requestedIp
        );
        mailService.sendInvitation(user, clinic, rawToken);
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        passwordPolicy.validate(request.newPassword());
        var now = Instant.now();
        var actionToken = tokenRepository.findByTokenHashAndUsedAtIsNull(hash(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou já utilizado"));
        if (!actionToken.getExpiresAt().isAfter(now)) {
            actionToken.setUsedAt(now);
            tokenRepository.save(actionToken);
            throw new IllegalArgumentException("Este link expirou. Solicite um novo acesso.");
        }
        var user = userRepository.findByIdAndClinicIdAndActiveTrue(
                actionToken.getUserId(),
                actionToken.getClinicId()
        ).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado ou inativo"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setCredentialsUpdatedAt(now);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        tokenRepository.invalidateOutstanding(user.getClinicId(), user.getId(), now);
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        passwordPolicy.validate(request.newPassword());
        var user = userRepository.findByIdAndClinicIdAndActiveTrue(
                TenantAccess.currentUserId(),
                TenantAccess.currentClinicId()
        ).orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Senha atual inválida");
        }
        updateCredentials(user, request.newPassword());
    }

    @Transactional
    public void revokeAllSessions() {
        var user = userRepository.findForUpdate(TenantAccess.currentUserId(), TenantAccess.currentClinicId())
                .orElseThrow(() -> new BadCredentialsException("Usuário não encontrado"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    public String createUnusableRandomPassword() {
        return passwordEncoder.encode(generateToken());
    }

    private void updateCredentials(AppUser user, String newPassword) {
        var now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setCredentialsUpdatedAt(now);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        tokenRepository.invalidateOutstanding(user.getClinicId(), user.getId(), now);
    }

    private String issue(
            AppUser user,
            AccountActionPurpose purpose,
            Duration duration,
            String requestedIp
    ) {
        var now = Instant.now();
        tokenRepository.invalidateOutstanding(user.getClinicId(), user.getId(), now);
        var rawToken = generateToken();
        var token = new AccountActionToken();
        token.setClinicId(user.getClinicId());
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setPurpose(purpose);
        token.setExpiresAt(now.plus(duration));
        token.setRequestedIp(requestedIp == null ? null : requestedIp.substring(0, Math.min(64, requestedIp.length())));
        tokenRepository.save(token);
        return rawToken;
    }

    private String generateToken() {
        var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.trim().getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
