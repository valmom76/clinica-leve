package br.com.clinicaleve.billing;

import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
public class AsaasWebhookController {
    private final AsaasWebhookAuthenticator authenticator;
    private final AsaasWebhookService service;

    @PostMapping
    ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(value = "asaas-access-token", required = false) String token,
            @RequestBody JsonNode payload
    ) {
        if (!authenticator.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("received", false, "message", "Token de webhook inválido"));
        }
        var outcome = service.handle(payload);
        if (!outcome.success()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("received", false, "message", outcome.error()));
        }
        return ResponseEntity.ok(Map.of("received", true, "duplicate", outcome.duplicate()));
    }
}
