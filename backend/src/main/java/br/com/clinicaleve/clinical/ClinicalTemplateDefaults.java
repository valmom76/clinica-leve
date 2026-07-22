package br.com.clinicaleve.clinical;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalTemplateDefaults {

    private final ClinicalDocumentTemplateRepository repository;

    public void ensureDefaults(String clinicId, String creatorUserId) {
        var existingNames = repository.findByClinicIdOrderByFavoriteDescNameAsc(clinicId)
                .stream()
                .map(template -> template.getName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());
        var defaults = List.of(
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.CLINICAL_REPORT,
                        "Relatório clínico",
                        "Relatório clínico - {{paciente.nome}}",
                        "Paciente: {{paciente.nome}}\nData de nascimento: {{paciente.data_nascimento}}\n\nRelato clínico:\n{{consulta.avaliacao}}\n\nConduta:\n{{consulta.plano}}\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        true
                ),
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.EXAM_REQUEST,
                        "Solicitação de exames",
                        "Solicitação de exames - {{paciente.nome}}",
                        "Solicito os exames abaixo para {{paciente.nome}}:\n\n[Descreva os exames solicitados]\n\nIndicação clínica:\n{{consulta.avaliacao}}\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        true
                ),
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.MEDICAL_CERTIFICATE,
                        "Atestado",
                        "Atestado - {{paciente.nome}}",
                        "Atesto, para os devidos fins, que {{paciente.nome}} esteve sob atendimento profissional em {{consulta.data}}.\n\n[Complete o período de afastamento e demais informações necessárias.]\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        false
                ),
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.ATTENDANCE_DECLARATION,
                        "Declaração de comparecimento",
                        "Declaração de comparecimento - {{paciente.nome}}",
                        "Declaro, para os devidos fins, que {{paciente.nome}} compareceu a esta clínica em {{consulta.data}} para atendimento.\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        false
                ),
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.PRESCRIPTION,
                        "Receita simples",
                        "Receita - {{paciente.nome}}",
                        "Paciente: {{paciente.nome}}\n\n[Informe o medicamento, a apresentação, a via, a dose, a frequência e a duração.]\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        false
                ),
                template(
                        clinicId,
                        creatorUserId,
                        ClinicalDocumentType.FREE_DOCUMENT,
                        "Documento livre",
                        "Documento - {{paciente.nome}}",
                        "Paciente: {{paciente.nome}}\nData: {{documento.data}}\n\n[Digite o conteúdo do documento.]\n\n{{profissional.nome}}\n{{profissional.conselho}}",
                        false
                )
        );
        repository.saveAll(defaults.stream()
                .filter(template -> !existingNames.contains(template.getName().toLowerCase()))
                .toList());
    }

    private ClinicalDocumentTemplate template(
            String clinicId,
            String creatorUserId,
            ClinicalDocumentType type,
            String name,
            String title,
            String body,
            boolean favorite
    ) {
        var template = new ClinicalDocumentTemplate();
        template.setClinicId(clinicId);
        template.setCreatedByUserId(creatorUserId);
        template.setType(type);
        template.setName(name);
        template.setTitleTemplate(title);
        template.setBodyTemplate(body);
        template.setFavorite(favorite);
        return template;
    }
}
