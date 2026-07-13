package br.com.clinicaleve.user;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.auth.Role;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.user.UserDtos.CreateUserRequest;
import br.com.clinicaleve.user.UserDtos.UpdateUserRequest;
import br.com.clinicaleve.user.UserDtos.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicUserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return repository.findByClinicIdOrderByActiveDescNameAsc(TenantAccess.currentClinicId())
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var email = normalizeEmail(request.email());
        if (repository.existsByClinicIdAndEmailIgnoreCase(clinicId, email)) {
            throw new IllegalArgumentException("Já existe um usuário com este e-mail na clínica");
        }

        var user = new AppUser();
        user.setClinicId(clinicId);
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setExpectedDailyMinutes(request.expectedDailyMinutes() == null ? 480 : request.expectedDailyMinutes());
        return UserResponse.from(repository.save(user));
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

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        if (request.expectedDailyMinutes() != null) {
            user.setExpectedDailyMinutes(request.expectedDailyMinutes());
        }
        user.setActive(request.active());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(repository.save(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
