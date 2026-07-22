package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document_signatures")
public class DocumentSignature extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String documentId;

    @Column(nullable = false, length = 36)
    private String credentialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SignatureMode mode;

    @Column(length = 80)
    private String providerKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentSignatureStatus status = DocumentSignatureStatus.PROCESSING;

    @Column(length = 500)
    private String signerSubject;

    @Column(length = 120)
    private String certificateSerial;

    @Column(length = 64)
    private String certificateFingerprint;

    @Column(nullable = false, length = 36)
    private String signedByUserId;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant signedAt;

    @Column(length = 500)
    private String signedPdfPath;

    @Column(length = 64)
    private String signedPdfHash;

    @Column(length = 1000)
    private String failureMessage;
}
