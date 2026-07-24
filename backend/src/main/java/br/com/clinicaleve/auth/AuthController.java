package br.com.clinicaleve.auth;

import br.com.clinicaleve.auth.AuthDtos.LoginRequest;
import br.com.clinicaleve.auth.AuthDtos.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;
    private final AccountMailService accountMailService;

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(authService.login(request, servletRequest.getRemoteAddr()));
    }

    @GetMapping("/capabilities")
    AuthDtos.AuthCapabilitiesResponse capabilities() {
        return new AuthDtos.AuthCapabilitiesResponse(accountMailService.isEnabled(), 10);
    }

    @PostMapping("/password/forgot")
    ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody AuthDtos.ForgotPasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        accountRecoveryService.requestPasswordReset(request, servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/password/reset")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        accountRecoveryService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/change")
    ResponseEntity<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        accountRecoveryService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    ResponseEntity<Void> logoutAll() {
        accountRecoveryService.revokeAllSessions();
        return ResponseEntity.noContent().build();
    }
}
