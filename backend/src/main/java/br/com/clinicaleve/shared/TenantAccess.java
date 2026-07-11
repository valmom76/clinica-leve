package br.com.clinicaleve.shared;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class TenantAccess {

    private TenantAccess() {
    }

    public static String currentClinicId() {
        return currentJwt().getClaimAsString("clinicId");
    }

    public static String currentUserId() {
        var subject = currentJwt().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Token sem usuário");
        }
        return subject;
    }

    private static Jwt currentJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Clínica não identificada");
        }

        var clinicId = jwt.getClaimAsString("clinicId");
        if (clinicId == null || clinicId.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Token sem clínica");
        }
        return jwt;
    }
}
