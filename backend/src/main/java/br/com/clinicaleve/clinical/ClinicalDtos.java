package br.com.clinicaleve.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import br.com.clinicaleve.clinical.signature.SignatureMode;

public final class ClinicalDtos {

    private ClinicalDtos() {
    }

    public record CreateEncounterRequest(@NotBlank String appointmentId) {
    }

    public record UpdateEncounterRequest(
            @NotNull Long lockVersion,
            @Size(max = 30000) String chiefComplaint,
            @Size(max = 30000) String subjectiveNotes,
            @Size(max = 30000) String objectiveNotes,
            @Size(max = 30000) String assessment,
            @Size(max = 30000) String carePlan,
            @Size(max = 30000) String additionalNotes
    ) {
    }

    public record EncounterResponse(
            String id,
            String appointmentId,
            String patientId,
            String patientName,
            String professionalId,
            String professionalName,
            String professionalCouncil,
            String specialtyId,
            EncounterStatus status,
            String chiefComplaint,
            String subjectiveNotes,
            String objectiveNotes,
            String assessment,
            String carePlan,
            String additionalNotes,
            String finalizedByUserId,
            Instant finalizedAt,
            long lockVersion,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record EncounterVersionResponse(
            String id,
            int versionNumber,
            EncounterStatus status,
            String chiefComplaint,
            String subjectiveNotes,
            String objectiveNotes,
            String assessment,
            String carePlan,
            String additionalNotes,
            String authorUserId,
            Instant createdAt
    ) {
    }

    public record TemplateRequest(
            @NotNull ClinicalDocumentType type,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 240) String titleTemplate,
            @NotBlank @Size(max = 30000) String bodyTemplate,
            boolean favorite,
            boolean active
    ) {
    }

    public record TemplateResponse(
            String id,
            ClinicalDocumentType type,
            String name,
            String titleTemplate,
            String bodyTemplate,
            boolean favorite,
            boolean active,
            int versionNumber,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record CreateDocumentRequest(
            @NotBlank String encounterId,
            @NotBlank String templateId
    ) {
    }

    public record UpdateDocumentRequest(
            @NotBlank @Size(max = 240) String title,
            @NotBlank @Size(max = 30000) String content
    ) {
    }

    public record DocumentResponse(
            String id,
            String encounterId,
            String patientId,
            String patientName,
            String professionalId,
            String professionalName,
            String professionalCouncil,
            String templateId,
            ClinicalDocumentType type,
            ClinicalDocumentStatus status,
            String title,
            String content,
            Integer templateVersion,
            int revisionNumber,
            String parentDocumentId,
            Instant finalizedAt,
            String documentHash,
            Instant signedAt,
            String signedPdfHash,
            String verificationCode,
            SignatureMode signatureMode,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record PlaceholderCatalog(String key, String description) {
    }

    public static List<PlaceholderCatalog> placeholders() {
        return List.of(
                new PlaceholderCatalog("{{clinica.nome}}", "Nome da clínica"),
                new PlaceholderCatalog("{{paciente.nome}}", "Nome do paciente"),
                new PlaceholderCatalog("{{paciente.cpf}}", "CPF do paciente"),
                new PlaceholderCatalog("{{paciente.data_nascimento}}", "Data de nascimento"),
                new PlaceholderCatalog("{{paciente.telefone}}", "Telefone do paciente"),
                new PlaceholderCatalog("{{paciente.email}}", "E-mail do paciente"),
                new PlaceholderCatalog("{{consulta.data}}", "Data do atendimento"),
                new PlaceholderCatalog("{{consulta.queixa_principal}}", "Queixa principal"),
                new PlaceholderCatalog("{{consulta.subjetivo}}", "Registro subjetivo"),
                new PlaceholderCatalog("{{consulta.objetivo}}", "Registro objetivo"),
                new PlaceholderCatalog("{{consulta.avaliacao}}", "Avaliação"),
                new PlaceholderCatalog("{{consulta.plano}}", "Plano/conduta"),
                new PlaceholderCatalog("{{consulta.observacoes}}", "Observações adicionais"),
                new PlaceholderCatalog("{{consulta.agendamento_observacoes}}", "Observações da agenda"),
                new PlaceholderCatalog("{{profissional.nome}}", "Nome do profissional"),
                new PlaceholderCatalog("{{profissional.conselho}}", "Conselho profissional"),
                new PlaceholderCatalog("{{documento.data}}", "Data de emissão")
        );
    }
}
