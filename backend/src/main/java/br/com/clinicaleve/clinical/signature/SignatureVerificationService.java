package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.ClinicalDocumentRepository;
import br.com.clinicaleve.clinical.signature.SignatureDtos.VerificationResponse;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignatureVerificationService {

    private final ClinicalDocumentRepository documentRepository;
    private final DocumentSignatureRepository signatureRepository;
    private final ClinicRepository clinicRepository;
    private final SignedPdfStorage storage;
    private final PdfSignatureVerifier verifier;
    private final CscProviderCatalog providerCatalog;

    @Transactional(readOnly = true)
    public VerificationResponse verify(String code) {
        var document = documentRepository.findByVerificationCode(code).orElse(null);
        if (document == null) {
            return notFound();
        }
        var clinic = clinicRepository.findById(document.getClinicId()).orElse(null);
        var signature = signatureRepository
                .findByDocumentIdAndClinicId(document.getId(), document.getClinicId())
                .orElse(null);
        if (signature == null || signature.getStatus() != DocumentSignatureStatus.SIGNED) {
            return new VerificationResponse(
                    true, false, false, false,
                    clinic == null ? null : clinic.getName(),
                    document.getType().name(),
                    signature == null ? null : signature.getMode(),
                    providerName(signature),
                    signature == null ? null : signature.getSignerSubject(),
                    signature == null ? null : signature.getCertificateSerial(),
                    signature == null ? null : signature.getCertificateFingerprint(),
                    null, null,
                    "A solicitação existe, mas a assinatura não foi concluída."
            );
        }
        var pdf = storage.read(signature.getSignedPdfPath());
        var integrity = storage.hash(pdf).equals(signature.getSignedPdfHash());
        var cryptographic = integrity && verifier.verify(pdf);
        return new VerificationResponse(
                true,
                true,
                integrity,
                cryptographic,
                clinic == null ? null : clinic.getName(),
                document.getType().name(),
                signature.getMode(),
                providerName(signature),
                signature.getSignerSubject(),
                signature.getCertificateSerial(),
                signature.getCertificateFingerprint(),
                signature.getSignedAt(),
                signature.getSignedPdfHash(),
                cryptographic
                        ? "Assinatura e integridade do arquivo confirmadas. Consulte também o VALIDAR do ITI para a cadeia ICP-Brasil e eventual revogação."
                        : "A verificação criptográfica local não foi concluída. Não utilize o documento sem validação adicional."
        );
    }

    private String providerName(DocumentSignature signature) {
        if (signature == null) return null;
        if (signature.getMode() == SignatureMode.LOCAL_PKCS12) return "Certificado A1";
        try {
            return providerCatalog.require(signature.getProviderKey()).name();
        } catch (RuntimeException ignored) {
            return signature.getProviderKey();
        }
    }

    private VerificationResponse notFound() {
        return new VerificationResponse(
                false, false, false, false,
                null, null, null, null, null, null, null, null, null,
                "Código de verificação não encontrado."
        );
    }
}
