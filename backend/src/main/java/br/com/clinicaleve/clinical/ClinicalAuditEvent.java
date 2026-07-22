package br.com.clinicaleve.clinical;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinical_audit_events")
public class ClinicalAuditEvent extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String actorUserId;

    @Column(nullable = false, length = 80, updatable = false)
    private String action;

    @Column(nullable = false, length = 60, updatable = false)
    private String entityType;

    @Column(nullable = false, length = 36, updatable = false)
    private String entityId;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String detailsJson;
}
