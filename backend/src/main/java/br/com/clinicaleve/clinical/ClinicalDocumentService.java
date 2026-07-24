package br.com.clinicaleve.clinical;

import br.com.clinicaleve.appointment.AppointmentRepository;
import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.clinical.ClinicalDtos.CreateDocumentRequest;
import br.com.clinicaleve.clinical.ClinicalDtos.DocumentResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.UpdateDocumentRequest;
import br.com.clinicaleve.patient.Patient;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.Professional;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import br.com.clinicaleve.clinical.signature.DocumentSignatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClinicalDocumentService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

    private final ClinicalDocumentRepository repository;
    private final ClinicalDocumentTemplateRepository templateRepository;
    private final ClinicalEncounterRepository encounterRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClinicRepository clinicRepository;
    private final ClinicalAccessService accessService;
    private final ClinicalAuditService auditService;
    private final TemplateRenderer renderer;
    private final DocumentSignatureRepository signatureRepository;

    @Transactional
    public List<DocumentResponse> list(String encounterId) {
        var encounter = loadEncounter(encounterId);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, encounter.getProfessionalId());
        var documents = repository.findByClinicIdAndEncounterIdOrderByCreatedAtDesc(
                        encounter.getClinicId(),
                        encounterId
                )
                .stream()
                .map(this::response)
                .toList();
        auditService.register(
                user,
                "ENCOUNTER_DOCUMENTS_VIEWED",
                "CLINICAL_ENCOUNTER",
                encounterId,
                "{\"documentCount\":" + documents.size() + "}"
        );
        return documents;
    }

    @Transactional
    public DocumentResponse get(String id) {
        var document = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, document.getProfessionalId());
        auditService.register(user, "DOCUMENT_VIEWED", "CLINICAL_DOCUMENT", id, null);
        return response(document);
    }

    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        var user = accessService.currentUser();
        var encounter = loadEncounter(request.encounterId());
        accessService.assertCanAccess(user, encounter.getProfessionalId());
        var template = templateRepository.findByIdAndClinicId(request.templateId(), encounter.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Modelo não encontrado"));
        if (!template.isActive()) {
            throw new IllegalStateException("Este modelo está inativo");
        }

        var values = renderValues(encounter);
        var document = new ClinicalDocument();
        document.setClinicId(encounter.getClinicId());
        document.setEncounterId(encounter.getId());
        document.setAppointmentId(encounter.getAppointmentId());
        document.setPatientId(encounter.getPatientId());
        document.setProfessionalId(encounter.getProfessionalId());
        document.setTemplateId(template.getId());
        document.setTemplateVersion(template.getVersionNumber());
        document.setType(template.getType());
        document.setTitle(renderer.render(template.getTitleTemplate(), values).trim());
        document.setContent(renderer.render(template.getBodyTemplate(), values).trim());
        document.setCreatedByUserId(user.getId());
        document.setUpdatedByUserId(user.getId());
        var saved = repository.save(document);
        auditService.register(user, "DOCUMENT_CREATED", "CLINICAL_DOCUMENT", saved.getId(), null);
        return response(saved);
    }

    @Transactional
    public DocumentResponse update(String id, UpdateDocumentRequest request) {
        var document = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, document.getProfessionalId());
        assertDraft(document);
        document.setTitle(request.title().trim());
        document.setContent(request.content().trim());
        document.setUpdatedByUserId(user.getId());
        var saved = repository.save(document);
        auditService.register(user, "DOCUMENT_UPDATED", "CLINICAL_DOCUMENT", saved.getId(), null);
        return response(saved);
    }

    @Transactional
    public DocumentResponse finalizeDocument(String id) {
        var document = load(id);
        var user = accessService.currentUser();
        accessService.assertCanFinalize(user, document.getProfessionalId());
        if (document.getStatus() == ClinicalDocumentStatus.FINALIZED
                || document.getStatus() == ClinicalDocumentStatus.SIGNED) {
            return response(document);
        }

        document.setStatus(ClinicalDocumentStatus.FINALIZED);
        document.setFinalizedAt(java.time.Instant.now());
        document.setFinalizedByUserId(user.getId());
        document.setUpdatedByUserId(user.getId());
        document.setDocumentHash(hash(document));
        var saved = repository.save(document);
        auditService.register(
                user,
                "DOCUMENT_FINALIZED",
                "CLINICAL_DOCUMENT",
                saved.getId(),
                "{\"sha256\":\"" + saved.getDocumentHash() + "\"}"
        );
        return response(saved);
    }

    @Transactional
    public DocumentResponse createRevision(String id) {
        var source = load(id);
        var user = accessService.currentUser();
        accessService.assertCanAccess(user, source.getProfessionalId());
        if (source.getStatus() == ClinicalDocumentStatus.DRAFT) {
            throw new IllegalStateException("Finalize o documento antes de criar uma nova revisão");
        }

        var revision = new ClinicalDocument();
        revision.setClinicId(source.getClinicId());
        revision.setEncounterId(source.getEncounterId());
        revision.setAppointmentId(source.getAppointmentId());
        revision.setPatientId(source.getPatientId());
        revision.setProfessionalId(source.getProfessionalId());
        revision.setTemplateId(source.getTemplateId());
        revision.setTemplateVersion(source.getTemplateVersion());
        revision.setType(source.getType());
        revision.setTitle(source.getTitle());
        revision.setContent(source.getContent());
        revision.setRevisionNumber(source.getRevisionNumber() + 1);
        revision.setParentDocumentId(source.getId());
        revision.setCreatedByUserId(user.getId());
        revision.setUpdatedByUserId(user.getId());
        var saved = repository.save(revision);
        auditService.register(
                user,
                "DOCUMENT_REVISION_CREATED",
                "CLINICAL_DOCUMENT",
                saved.getId(),
                "{\"parentDocumentId\":\"" + source.getId() + "\"}"
        );
        return response(saved);
    }

    private Map<String, String> renderValues(ClinicalEncounter encounter) {
        var clinic = clinicRepository.findById(encounter.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));
        var patient = patientRepository.findByIdAndClinicId(encounter.getPatientId(), encounter.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
        var professional = professionalRepository
                .findByIdAndClinicId(encounter.getProfessionalId(), encounter.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
        var appointment = appointmentRepository
                .findByIdAndClinicId(encounter.getAppointmentId(), encounter.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
        var zone = ZoneId.of(clinic.getTimezone());

        var values = new HashMap<String, String>();
        values.put("clinica.nome", clinic.getName());
        values.put("paciente.nome", patient.getName());
        values.put("paciente.cpf", patient.getCpf());
        values.put("paciente.data_nascimento", format(patient.getBirthDate()));
        values.put("paciente.telefone", patient.getPhone());
        values.put("paciente.email", patient.getEmail());
        values.put("consulta.data", DATE_FORMAT.format(appointment.getStartAt().atZone(zone)));
        values.put("consulta.queixa_principal", encounter.getChiefComplaint());
        values.put("consulta.subjetivo", encounter.getSubjectiveNotes());
        values.put("consulta.objetivo", encounter.getObjectiveNotes());
        values.put("consulta.avaliacao", encounter.getAssessment());
        values.put("consulta.plano", encounter.getCarePlan());
        values.put("consulta.observacoes", encounter.getAdditionalNotes());
        values.put("consulta.agendamento_observacoes", appointment.getNotes());
        values.put("profissional.nome", professional.getName());
        values.put("profissional.conselho", professional.getCouncil());
        values.put("documento.data", DATE_FORMAT.format(LocalDate.now(zone)));
        return values;
    }

    private ClinicalDocument load(String id) {
        return repository.findByIdAndClinicId(id, TenantAccess.currentClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
    }

    private ClinicalEncounter loadEncounter(String id) {
        return encounterRepository.findByIdAndClinicId(id, TenantAccess.currentClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Atendimento não encontrado"));
    }

    private DocumentResponse response(ClinicalDocument document) {
        var patient = patientRepository.findByIdAndClinicId(document.getPatientId(), document.getClinicId())
                .orElse(null);
        var professional = professionalRepository
                .findByIdAndClinicId(document.getProfessionalId(), document.getClinicId())
                .orElse(null);
        var signatureMode = signatureRepository
                .findByDocumentIdAndClinicId(document.getId(), document.getClinicId())
                .map(signature -> signature.getMode())
                .orElse(null);
        return new DocumentResponse(
                document.getId(),
                document.getEncounterId(),
                document.getPatientId(),
                patient == null ? "Paciente indisponível" : patient.getName(),
                document.getProfessionalId(),
                professional == null ? "Profissional indisponível" : professional.getName(),
                professional == null ? null : professional.getCouncil(),
                document.getTemplateId(),
                document.getType(),
                document.getStatus(),
                document.getTitle(),
                document.getContent(),
                document.getTemplateVersion(),
                document.getRevisionNumber(),
                document.getParentDocumentId(),
                document.getFinalizedAt(),
                document.getDocumentHash(),
                document.getSignedAt(),
                document.getSignedPdfHash(),
                document.getVerificationCode(),
                signatureMode,
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private void assertDraft(ClinicalDocument document) {
        if (document.getStatus() != ClinicalDocumentStatus.DRAFT) {
            throw new IllegalStateException(
                    "Documentos finalizados são imutáveis. Crie uma nova revisão para corrigir."
            );
        }
    }

    private String hash(ClinicalDocument document) {
        var canonical = String.join(
                "\n",
                document.getType().name(),
                document.getPatientId(),
                document.getProfessionalId(),
                Integer.toString(document.getRevisionNumber()),
                document.getTitle(),
                document.getContent()
        );
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private String format(LocalDate value) {
        return value == null ? null : DATE_FORMAT.format(value);
    }
}
