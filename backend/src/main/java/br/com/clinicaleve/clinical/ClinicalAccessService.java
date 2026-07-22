package br.com.clinicaleve.clinical;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.auth.AppUserRepository;
import br.com.clinicaleve.auth.Role;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClinicalAccessService {

    private final AppUserRepository userRepository;

    public AppUser currentUser() {
        var user = userRepository.findByIdAndClinicId(
                        TenantAccess.currentUserId(),
                        TenantAccess.currentClinicId()
                )
                .orElseThrow(() -> new AccessDeniedException("Usuário não pertence à clínica"));
        if (!user.isActive()) {
            throw new AccessDeniedException("Usuário inativo");
        }
        return user;
    }

    public void assertCanAccess(AppUser user, String professionalId) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.PROFESSIONAL
                && professionalId.equals(user.getProfessionalId())) {
            return;
        }
        throw new AccessDeniedException("Você não pode acessar este prontuário");
    }

    public void assertCanFinalize(AppUser user, String professionalId) {
        if (professionalId.equals(user.getProfessionalId())) {
            return;
        }
        throw new AccessDeniedException(
                "Somente o profissional responsável vinculado ao usuário pode finalizar"
        );
    }
}
