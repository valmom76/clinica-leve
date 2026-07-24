package br.com.clinicaleve.clinical;

import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.appointment.AppointmentStatus;
import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.clinical.ClinicalDtos.CreateEncounterRequest;
import br.com.clinicaleve.clinical.ClinicalDtos.EncounterResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.EncounterVersionResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.UpdateEncounterRequest;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ClinicalEncounterService {

    private final ClinicalEncounterRepository repository;
    private final ClinicalEncounterVersionRepository versionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClinicalAccessService accessService;
    private final ClinicalAuditService auditService;

    @Transactional
    public List<EncounterResponse> list(String patientId) {
        var clinicId = TenantAccess.currentClinicId();
        var user = accessService.currentUser();
        var encounters = patientId == null || patientId.isBlank()
                ? repository.findByClinicIdOrderByCreatedAtDesc(clinicId)
                : repository.findByClinicIdAndPatientIdOrderByCreatedAtDesc(clinicId, patientId);

        var response = encounters.stream()
                .filter(encounter -> user.getRole() == br.com.clinicaleve.auth.Role.ADMIN
                        || encounter.getProfessionalId().equals(user.getProfessionalId()))
                .map(this::response)
                .toList();
        if (patientId != null && !patientId.isBlank()) {
            auditService.register(
                    user,
                    "PATIENT_CLINICAL_HISTORY_VIEWED",
                    "PATIENT",
                    patientId,
                    "{\"encounterCount\":" + response.size() + "}"
            );
        }
        return response;
    }

    @Transactional
    public EncounterResponse get(String id) {
        var encounter = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, encounter.getProfessionalId());
        auditService.register(user, "ENCOUNTER_VIEWED", "CLINICAL_ENCOUNTER", id, null);
        return response(encounter);
    }

    @Transactional
    public EncounterResponse create(CreateEncounterRequest request) {
        var clinicId = TenantAccess.currentClinicId();
        var user = accessService.currentUser();
        var appointment = appointmentRepository.findByIdAndClinicId(request.appointmentId(), clinicId)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        accessService.assertCanAccess(user, appointment.getProfessionalId());

        var existing = repository.findByAppointmentIdAndClinicId(appointment.getId(), clinicId);
        if (existing.isPresent()) {
            return response(existing.get());
        }

        var encounter = new ClinicalEncounter();
        encounter.setClinicId(clinicId);
        encounter.setAppointmentId(appointment.getId());
        encounter.setPatientId(appointment.getPatientId());
        encounter.setProfessionalId(appointment.getProfessionalId());
        encounter.setSpecialtyId(appointment.getSpecialtyId());
        encounter.setCreatedByUserId(user.getId());
        encounter.setUpdatedByUserId(user.getId());
        var saved = repository.save(encounter);
        snapshot(saved, user);

        if (appointment.getStatus() == AppointmentStatus.SCHEDULED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED
                || appointment.getStatus() == AppointmentStatus.WAITING) {
            appointment.setStatus(AppointmentStatus.IN_PROGRESS);
            appointmentRepository.save(appointment);
        }
        auditService.register(user, "ENCOUNTER_CREATED", "CLINICAL_ENCOUNTER", saved.getId(), null);
        return response(saved);
    }

    @Transactional
    public EncounterResponse update(String id, UpdateEncounterRequest request) {
        var encounter = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, encounter.getProfessionalId());
        assertDraft(encounter);
        if (!Objects.equals(request.lockVersion(), encounter.getLockVersion())) {
            throw new IllegalStateException(
                    "Este atendimento foi alterado em outra sessão. Recarregue antes de salvar."
            );
        }

        encounter.setChiefComplaint(blankToNull(request.chiefComplaint()));
        encounter.setSubjectiveNotes(blankToNull(request.subjectiveNotes()));
        encounter.setObjectiveNotes(blankToNull(request.objectiveNotes()));
        encounter.setAssessment(blankToNull(request.assessment()));
        encounter.setCarePlan(blankToNull(request.carePlan()));
        encounter.setAdditionalNotes(blankToNull(request.additionalNotes()));
        encounter.setUpdatedByUserId(user.getId());
        var saved = repository.saveAndFlush(encounter);
        snapshot(saved, user);
        auditService.register(user, "ENCOUNTER_UPDATED", "CLINICAL_ENCOUNTER", saved.getId(), null);
        return response(saved);
    }

    @Transactional
    public EncounterResponse finalizeEncounter(String id) {
        var encounter = load(id);
        var user = accessService.currentUser();
        accessService.assertCanFinalize(user, encounter.getProfessionalId());
        if (encounter.getStatus() == EncounterStatus.FINALIZED) {
            return response(encounter);
        }
        if (!hasClinicalContent(encounter)) {
            throw new IllegalStateException("Preencha ao menos um campo clínico antes de finalizar");
        }

        encounter.setStatus(EncounterStatus.FINALIZED);
        encounter.setFinalizedByUserId(user.getId());
        encounter.setFinalizedAt(java.time.Instant.now());
        encounter.setUpdatedByUserId(user.getId());
        var saved = repository.saveAndFlush(encounter);
        snapshot(saved, user);

        appointmentRepository.findByIdAndClinicId(saved.getAppointmentId(), saved.getClinicId())
                .ifPresent(appointment -> {
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                    appointmentRepository.save(appointment);
                });
        auditService.register(
                user,
                "ENCOUNTER_FINALIZED",
                "CLINICAL_ENCOUNTER",
                saved.getId(),
                "{\"status\":\"FINALIZED\"}"
        );
        return response(saved);
    }

    @Transactional
    public List<EncounterVersionResponse> versions(String id) {
        var encounter = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, encounter.getProfessionalId());
        var versions = versionRepository
                .findByClinicIdAndEncounterIdOrderByVersionNumberDesc(encounter.getClinicId(), id)
                .stream()
                .map(version -> new EncounterVersionResponse(
                        version.getId(),
                        version.getVersionNumber(),
                        version.getStatus(),
                        version.getChiefComplaint(),
                        version.getSubjectiveNotes(),
                        version.getObjectiveNotes(),
                        version.getAssessment(),
                        version.getCarePlan(),
                        version.getAdditionalNotes(),
                        version.getAuthorUserId(),
                        version.getCreatedAt()
                ))
                .toList();
        auditService.register(
                user,
                "ENCOUNTER_HISTORY_VIEWED",
                "CLINICAL_ENCOUNTER",
                id,
                "{\"versionCount\":" + versions.size() + "}"
        );
        return versions;
    }

    private ClinicalEncounter load(String id) {
        return repository.findByIdAndClinicId(id, TenantAccess.currentClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado"));
    }

    private void snapshot(ClinicalEncounter encounter, AppUser user) {
        var version = new ClinicalEncounterVersion();
        version.setClinicId(encounter.getClinicId());
        version.setEncounterId(encounter.getId());
        version.setVersionNumber(Math.toIntExact(
                versionRepository.countByClinicIdAndEncounterId(encounter.getClinicId(), encounter.getId()) + 1
        ));
        version.setStatus(encounter.getStatus());
        version.setChiefComplaint(encounter.getChiefComplaint());
        version.setSubjectiveNotes(encounter.getSubjectiveNotes());
        version.setObjectiveNotes(encounter.getObjectiveNotes());
        version.setAssessment(encounter.getAssessment());
        version.setCarePlan(encounter.getCarePlan());
        version.setAdditionalNotes(encounter.getAdditionalNotes());
        version.setAuthorUserId(user.getId());
        versionRepository.save(version);
    }

    private EncounterResponse response(ClinicalEncounter encounter) {
        var patient = patientRepository.findByIdAndClinicId(encounter.getPatientId(), encounter.getClinicId())
                .orElse(null);
        var professional = professionalRepository
                .findByIdAndClinicId(encounter.getProfessionalId(), encounter.getClinicId())
                .orElse(null);
        return new EncounterResponse(
                encounter.getId(),
                encounter.getAppointmentId(),
                encounter.getPatientId(),
                patient == null ? "Paciente indisponível" : patient.getName(),
                encounter.getProfessionalId(),
                professional == null ? "Profissional indisponível" : professional.getName(),
                professional == null ? null : professional.getCouncil(),
                encounter.getSpecialtyId(),
                encounter.getStatus(),
                encounter.getChiefComplaint(),
                encounter.getSubjectiveNotes(),
                encounter.getObjectiveNotes(),
                encounter.getAssessment(),
                encounter.getCarePlan(),
                encounter.getAdditionalNotes(),
                encounter.getFinalizedByUserId(),
                encounter.getFinalizedAt(),
                encounter.getLockVersion(),
                encounter.getCreatedAt(),
                encounter.getUpdatedAt()
        );
    }

    private void assertDraft(ClinicalEncounter encounter) {
        if (encounter.getStatus() != EncounterStatus.DRAFT) {
            throw new IllegalStateException(
                    "Atendimentos finalizados são imutáveis. Consulte o histórico para auditoria."
            );
        }
    }

    private boolean hasClinicalContent(ClinicalEncounter encounter) {
        return Stream.of(
                        encounter.getChiefComplaint(),
                        encounter.getSubjectiveNotes(),
                        encounter.getObjectiveNotes(),
                        encounter.getAssessment(),
                        encounter.getCarePlan(),
                        encounter.getAdditionalNotes()
                )
                .anyMatch(value -> value != null && !value.isBlank());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
