package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.signature.SignatureDtos.CredentialResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.CscProviderResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.RemoteCredentialRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clinical/signatures/credentials")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSIONAL')")
public class SignatureCredentialController {

    private final SignatureCredentialService service;

    @GetMapping
    List<CredentialResponse> listMine() {
        return service.listMine();
    }

    @GetMapping("/remote-providers")
    List<CscProviderResponse> providers() {
        return service.providers();
    }

    @PostMapping("/local")
    @ResponseStatus(HttpStatus.CREATED)
    CredentialResponse uploadLocal(
            @RequestParam("file") MultipartFile file,
            @RequestParam String password,
            @RequestParam String displayName,
            @RequestParam boolean ownershipConfirmed
    ) {
        return service.uploadLocal(file, password, displayName, ownershipConfirmed);
    }

    @PostMapping("/remote")
    @ResponseStatus(HttpStatus.CREATED)
    CredentialResponse connectRemote(@Valid @RequestBody RemoteCredentialRequest request) {
        return service.connectRemote(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(@PathVariable String id) {
        service.deactivate(id);
    }
}
