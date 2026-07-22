package br.com.clinicaleve.clinical;

import br.com.clinicaleve.auth.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClinicalAuditService {

    private final ClinicalAuditEventRepository repository;

    public void register(
            AppUser actor,
            String action,
            String entityType,
            String entityId,
            String detailsJson
    ) {
        var event = new ClinicalAuditEvent();
        event.setClinicId(actor.getClinicId());
        event.setActorUserId(actor.getId());
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDetailsJson(detailsJson == null ? "{}" : detailsJson);
        repository.save(event);
    }
}
