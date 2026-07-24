package br.com.clinicaleve.shared;

import br.com.clinicaleve.billing.AsaasIntegrationException;
import br.com.clinicaleve.billing.SubscriptionPaymentRequiredException;
import br.com.clinicaleve.auth.LoginRateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        var fields = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos", fields);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Map<String, Object>> business(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(AsaasIntegrationException.class)
    ResponseEntity<Map<String, Object>> asaas(AsaasIntegrationException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage(), null);
    }

    @ExceptionHandler(SubscriptionPaymentRequiredException.class)
    ResponseEntity<Map<String, Object>> paymentRequired(SubscriptionPaymentRequiredException exception) {
        return response(HttpStatus.PAYMENT_REQUIRED, exception.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<Map<String, Object>> credentials() {
        return response(HttpStatus.UNAUTHORIZED, "Clínica, e-mail ou senha inválidos", null);
    }

    @ExceptionHandler(LoginRateLimitException.class)
    ResponseEntity<Map<String, Object>> loginRateLimit(LoginRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .body(body(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> response(
            HttpStatus status,
            String message,
            Object details
    ) {
        return ResponseEntity.status(status).body(body(status, message, details));
    }

    private Map<String, Object> body(HttpStatus status, String message, Object details) {
        var responseBody = new LinkedHashMap<String, Object>();
        responseBody.put("timestamp", Instant.now());
        responseBody.put("status", status.value());
        responseBody.put("message", message);
        if (details != null) {
            responseBody.put("details", details);
        }
        return responseBody;
    }
}
