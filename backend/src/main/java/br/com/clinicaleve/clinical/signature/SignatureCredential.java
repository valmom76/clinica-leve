package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "signature_credentials")
public class SignatureCredential extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String professionalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private SignatureMode mode;

    @Column(length = 80)
    private String providerKey;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 500, updatable = false)
    private String subjectName;

    @Column(length = 500, updatable = false)
    private String issuerName;

    @Column(nullable = false, length = 120, updatable = false)
    private String serialNumber;

    @Column(nullable = false, length = 64, updatable = false)
    private String fingerprintSha256;

    @Column(nullable = false, updatable = false)
    private Instant validFrom;

    @Column(nullable = false, updatable = false)
    private Instant validUntil;

    @Column(length = 240)
    private String remoteCredentialId;

    @Column(length = 20)
    private String remoteSecretKind;

    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] encryptedMaterial;

    @Column(nullable = false, columnDefinition = "VARBINARY(12)")
    private byte[] encryptionIv;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private boolean ownershipConfirmed;

    @Column(nullable = false, length = 36, updatable = false)
    private String createdByUserId;

    private Instant lastUsedAt;
}
