package br.com.clinicaleve.billing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class AsaasWebhookAuthenticator {
    private final AsaasProperties properties;

    public boolean isValid(String receivedToken) {
        var configured = properties.webhookToken();
        if (configured == null || configured.length() < 32 || receivedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configured.getBytes(StandardCharsets.UTF_8),
                receivedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
