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
@Table(name = "clinical_encounter_versions")
public class ClinicalEncounterVersion extends TenantEntity {

    @Column(nullable = false, length = 36, updatable = false)
    private String encounterId;

    @Column(nullable = false, updatable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private EncounterStatus status;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String chiefComplaint;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String subjectiveNotes;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String objectiveNotes;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String assessment;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String carePlan;

    @Column(columnDefinition = "LONGTEXT", updatable = false)
    private String additionalNotes;

    @Column(nullable = false, length = 36, updatable = false)
    private String authorUserId;
}
