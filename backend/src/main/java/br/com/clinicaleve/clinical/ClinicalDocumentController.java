package br.com.clinicaleve.clinical;

import br.com.clinicaleve.clinical.ClinicalDtos.CreateDocumentRequest;
import br.com.clinicaleve.clinical.ClinicalDtos.DocumentResponse;
import br.com.clinicaleve.clinical.ClinicalDtos.UpdateDocumentRequest;
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
@RequestMapping("/api/clinical/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
public class ClinicalDocumentController {

    private final ClinicalDocumentService service;

    @GetMapping
    List<DocumentResponse> list(@RequestParam String encounterId) {
        return service.list(encounterId);
    }

    @GetMapping("/{id}")
    DocumentResponse get(@PathVariable String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    DocumentResponse create(@Valid @RequestBody CreateDocumentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    DocumentResponse update(
            @PathVariable String id,
            @Valid @RequestBody UpdateDocumentRequest request
    ) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/finalize")
    DocumentResponse finalizeDocument(@PathVariable String id) {
        return service.finalizeDocument(id);
    }

    @PostMapping("/{id}/revisions")
    @ResponseStatus(HttpStatus.CREATED)
    DocumentResponse createRevision(@PathVariable String id) {
        return service.createRevision(id);
    }
}
