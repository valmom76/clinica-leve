package br.com.clinicaleve.clinical;

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
@Table(name = "clinical_documents")
public class ClinicalDocument extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String encounterId;

    @Column(nullable = false, length = 36)
    private String appointmentId;

    @Column(nullable = false, length = 36)
    private String patientId;

    @Column(nullable = false, length = 36)
    private String professionalId;

    @Column(length = 36)
    private String templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClinicalDocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicalDocumentStatus status = ClinicalDocumentStatus.DRAFT;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    private Integer templateVersion;

    @Column(nullable = false)
    private int revisionNumber = 1;

    @Column(length = 36)
    private String parentDocumentId;

    @Column(nullable = false, length = 36, updatable = false)
    private String createdByUserId;

    @Column(nullable = false, length = 36)
    private String updatedByUserId;

    @Column(length = 36)
    private String finalizedByUserId;

    private Instant finalizedAt;

    private Instant signedAt;

    @Column(length = 64)
    private String documentHash;

    @Column(length = 64)
    private String signedPdfHash;

    @Column(length = 64)
    private String verificationCode;
}
