package br.com.clinicaleve.appointment.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public record MessagingProperties(
        boolean enabled,
        String provider,
        String graphVersion,
        String phoneNumberId,
        String accessToken,
        String appSecret,
        String webhookVerifyToken
) {
    public MessagingProperties {
        provider = defaultIfBlank(provider, "META_WHATSAPP");
        graphVersion = defaultIfBlank(graphVersion, "v23.0");
    }

    public boolean metaConfigured() {
        return enabled
                && "META_WHATSAPP".equalsIgnoreCase(provider)
                && present(phoneNumberId)
                && present(accessToken)
                && present(appSecret)
                && present(webhookVerifyToken);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return present(value) ? value.trim() : fallback;
    }
}
