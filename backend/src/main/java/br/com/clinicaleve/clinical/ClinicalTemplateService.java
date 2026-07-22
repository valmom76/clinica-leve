package br.com.clinicaleve.clinical;

import br.com.clinicaleve.clinical.ClinicalDtos.TemplateRequest;
import br.com.clinicaleve.clinical.ClinicalDtos.TemplateResponse;
import br.com.clinicaleve.shared.TenantAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalTemplateService {

    private final ClinicalDocumentTemplateRepository repository;
    private final ClinicalAccessService accessService;
    private final ClinicalAuditService auditService;

    @Transactional(readOnly = true)
    public List<TemplateResponse> list() {
        accessService.currentUser();
        return repository.findByClinicIdOrderByFavoriteDescNameAsc(TenantAccess.currentClinicId())
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public TemplateResponse create(TemplateRequest request) {
        var user = accessService.currentUser();
        assertUniqueName(request.name(), "");
        var template = new ClinicalDocumentTemplate();
        template.setClinicId(user.getClinicId());
        template.setCreatedByUserId(user.getId());
        apply(template, request);
        var saved = repository.save(template);
        auditService.register(user, "TEMPLATE_CREATED", "CLINICAL_TEMPLATE", saved.getId(), null);
        return response(saved);
    }

    @Transactional
    public TemplateResponse update(String id, TemplateRequest request) {
        var user = accessService.currentUser();
        var template = repository.findByIdAndClinicId(id, user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Modelo não encontrado"));
        assertUniqueName(request.name(), id);
        apply(template, request);
        template.setVersionNumber(template.getVersionNumber() + 1);
        var saved = repository.save(template);
        auditService.register(
                user,
                "TEMPLATE_UPDATED",
                "CLINICAL_TEMPLATE",
                saved.getId(),
                "{\"version\":" + saved.getVersionNumber() + "}"
        );
        return response(saved);
    }

    private void apply(ClinicalDocumentTemplate template, TemplateRequest request) {
        template.setType(request.type());
        template.setName(request.name().trim());
        template.setTitleTemplate(request.titleTemplate().trim());
        template.setBodyTemplate(request.bodyTemplate().trim());
        template.setFavorite(request.favorite());
        template.setActive(request.active());
    }

    private void assertUniqueName(String name, String id) {
        if (repository.existsByClinicIdAndNameIgnoreCaseAndIdNot(
                TenantAccess.currentClinicId(),
                name.trim(),
                id
        )) {
            throw new IllegalArgumentException("Já existe um modelo com este nome");
        }
    }

    private TemplateResponse response(ClinicalDocumentTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getType(),
                template.getName(),
                template.getTitleTemplate(),
                template.getBodyTemplate(),
                template.isFavorite(),
                template.isActive(),
                template.getVersionNumber(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
