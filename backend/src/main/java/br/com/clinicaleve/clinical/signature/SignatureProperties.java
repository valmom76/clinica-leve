package br.com.clinicaleve.clinical.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.signature")
public record SignatureProperties(
        boolean enabled,
        String masterKey,
        String storageDirectory,
        String verificationBaseUrl,
        String cscProvidersJson
) {

    public SignatureProperties {
        storageDirectory = defaultIfBlank(storageDirectory, "./uploads/signed-documents");
        verificationBaseUrl = defaultIfBlank(
                verificationBaseUrl,
                "http://localhost:5173/verify"
        );
        cscProvidersJson = defaultIfBlank(cscProvidersJson, "[]");
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
