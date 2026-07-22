package br.com.clinicaleve.clinical.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CscProviderCatalog {

    private final SignatureProperties properties;
    private final ObjectMapper objectMapper;

    public List<CscProvider> providers() {
        var json = properties.cscProvidersJson();
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        try {
            return Arrays.stream(objectMapper.readValue(json, CscProvider[].class))
                    .map(this::validated)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalStateException("SIGNATURE_CSC_PROVIDERS_JSON inválido", exception);
        }
    }

    public CscProvider require(String key) {
        return providers().stream()
                .filter(provider -> provider.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provedor remoto não configurado"));
    }

    private CscProvider validated(CscProvider provider) {
        if (provider.key() == null || provider.key().isBlank()
                || provider.name() == null || provider.name().isBlank()
                || provider.baseUrl() == null || provider.baseUrl().isBlank()) {
            throw new IllegalStateException("Provedor CSC sem key, name ou baseUrl");
        }
        var uri = URI.create(provider.baseUrl());
        var local = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !local) {
            throw new IllegalStateException("O provedor CSC precisa usar HTTPS");
        }
        return new CscProvider(provider.key().trim(), provider.name().trim(), stripSlash(provider.baseUrl()));
    }

    private String stripSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record CscProvider(String key, String name, String baseUrl) {
    }
}
