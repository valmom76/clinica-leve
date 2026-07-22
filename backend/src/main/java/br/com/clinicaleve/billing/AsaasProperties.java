package br.com.clinicaleve.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.asaas")
public record AsaasProperties(
        boolean enabled,
        String environment,
        String apiKey,
        String webhookToken,
        String frontendUrl
) {
    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String baseUrl() {
        return "production".equalsIgnoreCase(environment)
                ? "https://api.asaas.com/v3"
                : "https://api-sandbox.asaas.com/v3";
    }

    public String normalizedEnvironment() {
        return "production".equalsIgnoreCase(environment) ? "production" : "sandbox";
    }

    public String callbackUrl(String result) {
        var base = frontendUrl == null || frontendUrl.isBlank()
                ? "http://localhost:5173"
                : frontendUrl.replaceAll("/+$", "");
        return base + "?billing=" + result;
    }
}
