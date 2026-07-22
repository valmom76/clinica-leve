package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.signature.SignatureDtos.VerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/signatures")
@RequiredArgsConstructor
public class PublicSignatureVerificationController {

    private final SignatureVerificationService service;

    @GetMapping("/verify/{code}")
    VerificationResponse verify(@PathVariable String code) {
        return service.verify(code);
    }
}
