package br.com.clinicaleve.shared;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class TenantAccess {

    private TenantAccess() {
    }

    public static String currentClinicId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Clínica não identificada");
        }

        var clinicId = jwt.getClaimAsString("clinicId");
        if (clinicId == null || clinicId.isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Token sem clínica");
        }
        return clinicId;
    }
}
