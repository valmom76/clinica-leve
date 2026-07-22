package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.ClinicalAccessService;
import br.com.clinicaleve.clinical.ClinicalAuditService;
import br.com.clinicaleve.clinical.ClinicalDocumentRepository;
import br.com.clinicaleve.clinical.ClinicalDocumentStatus;
import br.com.clinicaleve.clinical.signature.SignatureDtos.DocumentSignatureResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.SignDocumentRequest;
import br.com.clinicaleve.patient.PatientRepository;
import br.com.clinicaleve.professional.ProfessionalRepository;
import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DocumentSignatureService {

    private final ClinicalDocumentRepository documentRepository;
    private final DocumentSignatureRepository signatureRepository;
    private final SignatureCredentialService credentialService;
    private final ClinicalAccessService accessService;
    private final SignatureCryptoService cryptoService;
    private final SignatureProviderRegistry providerRegistry;
    private final ClinicalPdfGenerator pdfGenerator;
    private final PdfPadesSigner pdfSigner;
    private final SignedPdfStorage storage;
    private final ClinicRepository clinicRepository;
    private final PatientRepository patientRepository;
    private final ProfessionalRepository professionalRepository;
    private final ClinicalAuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SignatureProperties properties;

    @Transactional
    public DocumentSignatureResponse sign(String documentId, SignDocumentRequest request) {
        cryptoService.requireConfigured();
        var user = accessService.currentUser();
        var document = documentRepository.findByIdAndClinicId(documentId, user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        accessService.assertCanFinalize(user, document.getProfessionalId());
        if (document.getStatus() == ClinicalDocumentStatus.DRAFT) {
            throw new IllegalStateException("Finalize o documento antes de assiná-lo");
        }
        var existing = signatureRepository.findByDocumentIdAndClinicId(documentId, user.getClinicId())
                .orElse(null);
        if (existing != null && existing.getStatus() == DocumentSignatureStatus.SIGNED) {
            return response(existing, document.getVerificationCode());
        }
        var credential = credentialService.requireOwnedActive(request.credentialId(), user);
        if (!credential.getProfessionalId().equals(document.getProfessionalId())) {
            throw new IllegalStateException("O certificado não pertence ao profissional responsável");
        }

        var signature = existing == null ? new DocumentSignature() : existing;
        if (existing == null) {
            signature.setClinicId(user.getClinicId());
            signature.setDocumentId(document.getId());
        }
        signature.setRequestedAt(Instant.now());
        signature.setSignedByUserId(user.getId());
        signature.setCredentialId(credential.getId());
        signature.setMode(credential.getMode());
        signature.setProviderKey(credential.getProviderKey());
        signature.setSignerSubject(credential.getSubjectName());
        signature.setCertificateSerial(credential.getSerialNumber());
        signature.setCertificateFingerprint(credential.getFingerprintSha256());
        signature.setStatus(DocumentSignatureStatus.PROCESSING);
        signature.setFailureMessage(null);
        signature = signatureRepository.save(signature);

        if (document.getVerificationCode() == null) {
            document.setVerificationCode(verificationCode());
        }
        var signingTime = Instant.now();
        try {
            var clinic = clinicRepository.findById(user.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));
            var patient = patientRepository.findByIdAndClinicId(document.getPatientId(), user.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado"));
            var professional = professionalRepository
                    .findByIdAndClinicId(document.getProfessionalId(), user.getClinicId())
                    .orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado"));
            var verificationUrl = verificationUrl(document.getVerificationCode());
            var unsignedPdf = pdfGenerator.generate(
                    document,
                    clinic,
                    patient,
                    professional,
                    credential,
                    document.getVerificationCode(),
                    signingTime,
                    verificationUrl
            );
            var identity = providerRegistry.require(credential.getMode())
                    .prepare(credential, request.secret(), request.secondarySecret());
            var signedPdf = pdfSigner.sign(unsignedPdf, credential.getSubjectName(), identity);
            var stored = storage.save(
                    user.getClinicId(),
                    document.getId(),
                    signature.getId(),
                    signedPdf
            );

            signature.setStatus(DocumentSignatureStatus.SIGNED);
            signature.setSignedAt(signingTime);
            signature.setSignedPdfPath(stored.relativePath());
            signature.setSignedPdfHash(stored.sha256());
            credential.setLastUsedAt(signingTime);
            document.setStatus(ClinicalDocumentStatus.SIGNED);
            document.setSignedAt(signingTime);
            document.setSignedPdfHash(stored.sha256());
            documentRepository.save(document);
            signatureRepository.save(signature);
            auditService.register(
                    user,
                    "DOCUMENT_DIGITALLY_SIGNED",
                    "CLINICAL_DOCUMENT",
                    document.getId(),
                    "{\"mode\":\"" + credential.getMode() + "\",\"pdfSha256\":\"" + stored.sha256() + "\"}"
            );
        } catch (RuntimeException exception) {
            signature.setStatus(DocumentSignatureStatus.FAILED);
            signature.setFailureMessage(safeFailure(exception));
            signatureRepository.save(signature);
        }
        return response(signature, document.getVerificationCode());
    }

    @Transactional(readOnly = true)
    public DocumentSignatureResponse get(String documentId) {
        var user = accessService.currentUser();
        var document = documentRepository.findByIdAndClinicId(documentId, user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        accessService.assertCanAccess(user, document.getProfessionalId());
        var signature = signatureRepository.findByDocumentIdAndClinicId(documentId, user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Documento ainda não possui assinatura digital"));
        return response(signature, document.getVerificationCode());
    }

    @Transactional(readOnly = true)
    public SignedPdf download(String documentId) {
        var user = accessService.currentUser();
        var document = documentRepository.findByIdAndClinicId(documentId, user.getClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado"));
        accessService.assertCanAccess(user, document.getProfessionalId());
        var signature = signatureRepository.findByDocumentIdAndClinicId(documentId, user.getClinicId())
                .filter(item -> item.getStatus() == DocumentSignatureStatus.SIGNED)
                .orElseThrow(() -> new IllegalStateException("Documento ainda não foi assinado"));
        return new SignedPdf(
                storage.read(signature.getSignedPdfPath()),
                safeFilename(document.getTitle()) + "-assinado.pdf"
        );
    }

    private DocumentSignatureResponse response(DocumentSignature signature, String verificationCode) {
        return new DocumentSignatureResponse(
                signature.getId(),
                signature.getDocumentId(),
                signature.getCredentialId(),
                signature.getMode(),
                signature.getProviderKey(),
                signature.getStatus(),
                signature.getSignerSubject(),
                signature.getCertificateSerial(),
                signature.getCertificateFingerprint(),
                signature.getSignedAt(),
                signature.getSignedPdfHash(),
                verificationCode,
                signature.getFailureMessage()
        );
    }

    private String verificationCode() {
        var bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String verificationUrl(String code) {
        var base = properties.verificationBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/" + code;
    }

    private String safeFailure(RuntimeException exception) {
        var message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return "Não foi possível concluir a assinatura";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private String safeFilename(String value) {
        var normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        return normalized.isBlank() ? "documento-clinico" : normalized;
    }

    public record SignedPdf(byte[] content, String filename) {
    }
}
