package br.com.clinicaleve.appointment.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class WhatsAppWebhookSecurity {

    private final MessagingProperties properties;

    public boolean verifyToken(String mode, String token) {
        return "subscribe".equals(mode)
                && present(properties.webhookVerifyToken())
                && MessageDigest.isEqual(
                        properties.webhookVerifyToken().getBytes(StandardCharsets.UTF_8),
                        value(token).getBytes(StandardCharsets.UTF_8)
                );
    }

    public boolean verifySignature(byte[] payload, String signature) {
        if (!present(properties.appSecret()) || !present(signature)
                || !signature.startsWith("sha256=")) return false;
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.appSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            var expected = HexFormat.of().formatHex(mac.doFinal(payload));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signature.substring("sha256=".length()).getBytes(StandardCharsets.US_ASCII)
            );
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
