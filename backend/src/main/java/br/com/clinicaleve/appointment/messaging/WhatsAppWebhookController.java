package br.com.clinicaleve.appointment.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppWebhookSecurity security;
    private final WhatsAppWebhookService service;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (!security.verifyToken(mode, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid token");
        }
        return ResponseEntity.ok(challenge == null ? "" : challenge);
    }

    @PostMapping
    ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] payload
    ) {
        if (!security.verifySignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.handle(payload);
        return ResponseEntity.ok().build();
    }
}
