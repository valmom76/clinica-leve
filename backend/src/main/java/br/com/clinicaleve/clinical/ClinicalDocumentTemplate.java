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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinical_document_templates")
public class ClinicalDocumentTemplate extends TenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ClinicalDocumentType type;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, length = 240)
    private String titleTemplate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String bodyTemplate;

    @Column(nullable = false)
    private boolean favorite;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int versionNumber = 1;

    @Column(length = 36, updatable = false)
    private String createdByUserId;
}
