package br.com.clinicaleve.user;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.auth.Role;
import br.com.clinicaleve.auth.AccountRecoveryService;
import br.com.clinicaleve.auth.PasswordPolicy;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.user.UserDtos.CreateUserRequest;
import br.com.clinicaleve.user.UserDtos.UpdateUserRequest;
import br.com.clinicaleve.user.UserDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClinicUserService {

    private final AppUserRepository repository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AccountRecoveryService accountRecoveryService;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return repository.findByClinicIdOrderByActiveDescNameAsc(TenantAccess.currentClinicId())
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, String requestedIp) {
        var clinicId = TenantAccess.currentClinicId();
        var email = normalizeEmail(request.email());
        if (repository.existsByClinicIdAndEmailIgnoreCase(clinicId, email)) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail na clínica");
        }

        var user = new AppUser();
        user.setClinicId(clinicId);
        user.setName(request.name().trim());
        user.setEmail(email);
        if (request.sendInvitation()) {
            user.setPasswordHash(accountRecoveryService.createUnusableRandomPassword());
        } else {
            if (request.password() == null || request.password().isBlank()) {
                throw new IllegalArgumentException("Informe uma senha ou envie um convite por e-mail");
            }
            passwordPolicy.validate(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setCredentialsUpdatedAt(Instant.now());
        }
        user.setRole(request.role());
        user.setProfessionalId(validateProfessionalLink(clinicId, request.role(), request.professionalId(), null));
        user.setExpectedDailyMinutes(request.expectedDailyMinutes() == null ? 480 : request.expectedDailyMinutes());
        var saved = repository.save(user);
        if (request.sendInvitation()) {
            accountRecoveryService.sendInvitation(saved, requestedIp);
        }
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(String id, UpdateUserRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var user = repository.findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        var email = normalizeEmail(request.email());

        if (repository.existsByClinicIdAndEmailIgnoreCaseAndIdNot(clinicId, email, id)) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail na clínica");
        }

        var isCurrentUser = id.equals(TenantAccess.currentUserId());
        if (isCurrentUser && (!request.active() || request.role() != Role.ADMIN)) {
            throw new IllegalStateException("O administrador conectado não pode remover o próprio acesso");
        }

        var removesActiveAdmin = user.isActive()
                && user.getRole() == Role.ADMIN
                && (!request.active() || request.role() != Role.ADMIN);
        if (removesActiveAdmin
                && repository.countByClinicIdAndRoleAndActiveTrue(clinicId, Role.ADMIN) <= 1) {
            throw new IllegalStateException("A clínica precisa manter pelo menos um administrador ativo");
        }

        var securityChanged = !user.getEmail().equalsIgnoreCase(email)
                || user.getRole() != request.role()
                || user.isActive() != request.active()
                || !Objects.equals(user.getProfessionalId(), normalizedProfessionalId(request.professionalId()));

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setProfessionalId(validateProfessionalLink(clinicId, request.role(), request.professionalId(), id));
        if (request.expectedDailyMinutes() != null) {
            user.setExpectedDailyMinutes(request.expectedDailyMinutes());
        }
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            passwordPolicy.validate(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setCredentialsUpdatedAt(Instant.now());
            securityChanged = true;
        }
        if (securityChanged) {
            user.setTokenVersion(user.getTokenVersion() + 1);
        }
        return UserResponse.from(repository.save(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String validateProfessionalLink(String clinicId, Role role, String professionalId, String userId) {
        var normalizedId = normalizedProfessionalId(professionalId);
        if (role == Role.PROFESSIONAL && normalizedId == null) {
            throw new IllegalArgumentException("O perfil profissional precisa estar vinculado a um profissional da clínica");
        }
        if (normalizedId == null) {
            return null;
        }
        if (role != Role.PROFESSIONAL && role != Role.ADMIN) {
            throw new IllegalArgumentException("Somente profissionais e administradores podem ter vínculo clínico");
        }
        var professional = professionalRepository.findByIdAndClinicId(normalizedId, clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado nesta clínica"));
        if (!professional.isActive()) {
            throw new IllegalArgumentException("Não é possível vincular um profissional inativo");
        }
        var comparisonId = userId == null ? "" : userId;
        if (repository.existsByClinicIdAndProfessionalIdAndIdNot(clinicId, normalizedId, comparisonId)) {
            throw new IllegalArgumentException("Este profissional já está vinculado a outro usuário");
        }
        return normalizedId;
    }

    private String normalizedProfessionalId(String professionalId) {
        return professionalId == null || professionalId.isBlank() ? null : professionalId.trim();
    }
}
