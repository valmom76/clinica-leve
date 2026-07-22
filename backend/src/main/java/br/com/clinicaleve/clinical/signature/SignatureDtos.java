package br.com.clinicaleve.clinical.signature;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SignatureDtos {

    private SignatureDtos() {
    }

    public record CredentialResponse(
            String id,
            SignatureMode mode,
            String providerKey,
            String providerName,
            String displayName,
            String subjectName,
            String issuerName,
            String serialNumber,
            String fingerprintSha256,
            Instant validFrom,
            Instant validUntil,
            String remoteSecretKind,
            boolean active,
            boolean ownershipConfirmed,
            Instant lastUsedAt
    ) {
    }

    public record CscProviderResponse(String key, String name) {
    }

    public record RemoteCredentialRequest(
            @NotBlank String providerKey,
            @NotBlank @Size(max = 240) String credentialId,
            @NotBlank @Size(max = 8000) String accessToken,
            @NotBlank @Size(max = 160) String displayName,
            @AssertTrue(message = "Confirme que o certificado pertence ao profissional conectado")
            boolean ownershipConfirmed
    ) {
    }

    public record SignDocumentRequest(
            @NotBlank String credentialId,
            @NotBlank @Size(max = 300) String secret,
            @Size(max = 300) String secondarySecret
    ) {
    }

    public record DocumentSignatureResponse(
            String id,
            String documentId,
            String credentialId,
            SignatureMode mode,
            String providerKey,
            DocumentSignatureStatus status,
            String signerSubject,
            String certificateSerial,
            String certificateFingerprint,
            Instant signedAt,
            String signedPdfHash,
            String verificationCode,
            String failureMessage
    ) {
    }

    public record VerificationResponse(
            boolean found,
            boolean signed,
            boolean integrityValid,
            boolean cryptographicSignatureValid,
            String clinicName,
            String documentType,
            SignatureMode mode,
            String providerName,
            String signerSubject,
            String certificateSerial,
            String certificateFingerprint,
            Instant signedAt,
            String signedPdfHash,
            String notice
    ) {
    }
}
