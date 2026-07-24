package br.com.clinicaleve.auth;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;

@Component
@RequiredArgsConstructor
public class UserSessionValidationFilter extends OncePerRequestFilter {

    private final AppUserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            var clinicId = jwt.getClaimAsString("clinicId");
            var userId = jwt.getSubject();
            var tokenVersion = jwt.getClaim("tokenVersion");
            var expectedVersion = tokenVersion instanceof Number number ? number.intValue() : -1;
            var valid = clinicId != null && userId != null && userRepository
                    .findByIdAndClinicIdAndActiveTrue(userId, clinicId)
                    .map(user -> user.getTokenVersion() == expectedVersion)
                    .orElse(false);
            if (!valid) {
                SecurityContextHolder.clearContext();
                unauthorized(response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("message", "Sua sessão expirou ou foi encerrada. Entre novamente.");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
