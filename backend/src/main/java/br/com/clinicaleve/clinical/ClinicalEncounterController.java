package br.com.clinicaleve.clinical;

import br.com.clinicaleve.clinical.ClinicalDtos.CreateEncounterRequest;
import br.com.clinicaleve.clinical.ClinicalDtos.EncounterResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.EncounterVersionResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.UpdateEncounterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clinical/encounters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
public class ClinicalEncounterController {

    private final ClinicalEncounterService service;

    @GetMapping
    List<EncounterResponse> list(@RequestParam(required = false) String patientId) {
        return service.list(patientId);
    }

    @GetMapping("/{id}")
    EncounterResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EncounterResponse create(@Valid @RequestBody CreateEncounterRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    EncounterResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateEncounterRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/finalize")
    EncounterResponse finalizeEncounter(@PathVariable String id) {
        return service.finalizeEncounter(id);
    }

    @GetMapping("/{id}/versions")
    List<EncounterVersionResponse> versions(@PathVariable String id) {
        return service.versions(id);
    }
}
