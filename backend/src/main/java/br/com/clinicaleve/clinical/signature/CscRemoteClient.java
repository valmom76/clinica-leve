package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.signature.CertificateMaterialService.CertificateMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CscRemoteClient {

    private static final String SHA256_OID = "2.16.840.1.101.3.4.2.1";
    private static final String RSA_SHA256_OID = "1.2.840.113549.1.1.11";
    private static final String ECDSA_SHA256_OID = "1.2.840.10045.4.3.2";

    private final CscProviderCatalog catalog;
    private final CertificateMaterialService certificateMaterialService;

    public RemoteCredentialInfo credentialInfo(
            String providerKey,
            String accessToken,
            String credentialId
    ) {
        var body = post(
                catalog.require(providerKey),
                accessToken,
                "/credentials/info",
                Map.of("credentialID", credentialId, "certificates", "chain")
        );
        var certificateNodes = body.path("cert").path("certificates");
        if (!certificateNodes.isArray() || certificateNodes.isEmpty()) {
            throw new IllegalArgumentException("O provedor remoto não retornou a cadeia do certificado");
        }
        var encodedChain = new ArrayList<byte[]>();
        certificateNodes.forEach(node -> encodedChain.add(Base64.getDecoder().decode(node.asText())));
        if ("implicit".equalsIgnoreCase(body.path("authMode").asText(""))) {
            throw new IllegalArgumentException(
                    "Este provedor usa autorização implícita e precisa de um adaptador próprio"
            );
        }
        var metadata = certificateMaterialService.fromDerChain(encodedChain);
        return new RemoteCredentialInfo(
                metadata,
                secretKind(body),
                List.copyOf(encodedChain)
        );
    }

    public byte[] signHash(
            String providerKey,
            String accessToken,
            String credentialId,
            String secretKind,
            String secret,
            String signatureAlgorithm,
            String secondarySecret,
            byte[] digest
    ) {
        var provider = catalog.require(providerKey);
        var authorization = new LinkedHashMap<String, Object>();
        authorization.put("credentialID", credentialId);
        authorization.put("numSignatures", 1);
        addAuthorizationSecrets(authorization, secretKind, secret, secondarySecret);
        var authorizationResponse = post(
                provider,
                accessToken,
                "/credentials/authorize",
                authorization
        );
        var sad = requiredText(authorizationResponse, "SAD", "O provedor não autorizou a assinatura");

        var signRequest = new LinkedHashMap<String, Object>();
        signRequest.put("credentialID", credentialId);
        signRequest.put("SAD", sad);
        signRequest.put("hash", List.of(Base64.getEncoder().encodeToString(digest)));
        signRequest.put("hashAlgo", SHA256_OID);
        signRequest.put("signAlgo", signatureAlgorithm.contains("ECDSA")
                ? ECDSA_SHA256_OID
                : RSA_SHA256_OID);
        var signatureResponse = post(
                provider,
                accessToken,
                "/signatures/signHash",
                signRequest
        );
        var signatures = signatureResponse.path("signatures");
        if (!signatures.isArray() || signatures.isEmpty()) {
            throw new IllegalStateException("O provedor remoto não retornou a assinatura");
        }
        return Base64.getDecoder().decode(signatures.get(0).asText());
    }

    private JsonNode post(
            CscProviderCatalog.CscProvider provider,
            String accessToken,
            String path,
            Map<String, ?> body
    ) {
        try {
            var response = RestClient.create(provider.baseUrl())
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("O provedor remoto retornou uma resposta vazia");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "O provedor remoto recusou a operação (HTTP " + exception.getStatusCode().value() + ")",
                    exception
            );
        }
    }

    private String secretKind(JsonNode info) {
        var pin = present(info.path("PIN").path("presence"));
        var otp = present(info.path("OTP").path("presence"));
        if (pin && otp) return "PIN_OTP";
        if (otp) return "OTP";
        return "PIN";
    }

    private void addAuthorizationSecrets(
            Map<String, Object> authorization,
            String secretKind,
            String secret,
            String secondarySecret
    ) {
        if ("OTP".equals(secretKind)) {
            authorization.put("OTP", secret);
            return;
        }
        authorization.put("PIN", secret);
        if ("PIN_OTP".equals(secretKind)) {
            if (secondarySecret == null || secondarySecret.isBlank()) {
                throw new IllegalArgumentException("Informe também o código OTP do provedor");
            }
            authorization.put("OTP", secondarySecret);
        }
    }

    private boolean present(JsonNode node) {
        var value = node.asText("");
        return "true".equalsIgnoreCase(value) || "required".equalsIgnoreCase(value);
    }

    private String requiredText(JsonNode node, String field, String message) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).asText().isBlank()) {
            throw new IllegalStateException(message);
        }
        return node.path(field).asText();
    }

    public record RemoteCredentialInfo(
            CertificateMetadata metadata,
            String secretKind,
            List<byte[]> certificateChain
    ) {
    }
}
