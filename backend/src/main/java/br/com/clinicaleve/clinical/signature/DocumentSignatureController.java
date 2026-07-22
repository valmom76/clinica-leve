package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.signature.SignatureDtos.DocumentSignatureResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.SignDocumentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/clinical/signatures/documents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
public class DocumentSignatureController {

    private final DocumentSignatureService service;

    @PostMapping("/{documentId}")
    DocumentSignatureResponse sign(
            @PathVariable String documentId,
            @Valid @RequestBody SignDocumentRequest request
    ) {
        return service.sign(documentId, request);
    }

    @GetMapping("/{documentId}")
    DocumentSignatureResponse get(@PathVariable String documentId) {
        return service.get(documentId);
    }

    @GetMapping(value = "/{documentId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    ResponseEntity<byte[]> download(@PathVariable String documentId) {
        var pdf = service.download(documentId);
        var disposition = ContentDisposition.attachment()
                .filename(pdf.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.content().length)
                .body(pdf.content());
    }
}
