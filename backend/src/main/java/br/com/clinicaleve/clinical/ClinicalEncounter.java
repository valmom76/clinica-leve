package br.com.clinicaleve.clinical;

import br.com.clinicaleve.shared.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clinical_encounters")
public class ClinicalEncounter extends TenantEntity {

    @Column(nullable = false, length = 36)
    private String appointmentId;

    @Column(nullable = false, length = 36)
    private String patientId;

    @Column(nullable = false, length = 36)
    private String professionalId;

    @Column(nullable = false, length = 36)
    private String specialtyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EncounterStatus status = EncounterStatus.DRAFT;

    @Column(columnDefinition = "LONGTEXT")
    private String chiefComplaint;

    @Column(columnDefinition = "LONGTEXT")
    private String subjectiveNotes;

    @Column(columnDefinition = "LONGTEXT")
    private String objectiveNotes;

    @Column(columnDefinition = "LONGTEXT")
    private String assessment;

    @Column(columnDefinition = "LONGTEXT")
    private String carePlan;

    @Column(columnDefinition = "LONGTEXT")
    private String additionalNotes;

    @Column(nullable = false, length = 36, updatable = false)
    private String createdByUserId;

    @Column(nullable = false, length = 36)
    private String updatedByUserId;

    @Column(length = 36)
    private String finalizedByUserId;

    private Instant finalizedAt;

    @Version
    @Column(nullable = false)
    private long lockVersion;
}
