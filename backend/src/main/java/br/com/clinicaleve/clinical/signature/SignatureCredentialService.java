package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.auth.AppUser;
import br.com.clinicaleve.clinical.ClinicalAccessService;
import br.com.clinicaleve.clinical.signature.SignatureDtos.CredentialResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.CscProviderResponse;
import br.com.clinicaleve.clinical.signature.SignatureDtos.RemoteCredentialRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SignatureCredentialService {

    private static final long MAX_PKCS12_BYTES = 2L * 1024 * 1024;

    private final SignatureCredentialRepository repository;
    private final ClinicalAccessService accessService;
    private final SignatureCryptoService cryptoService;
    private final CertificateMaterialService certificateMaterialService;
    private final CscProviderCatalog providerCatalog;
    private final CscRemoteClient remoteClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CredentialResponse> listMine() {
        var user = professionalUser();
        return repository.findByClinicIdAndProfessionalIdOrderByActiveDescCreatedAtDesc(
                        user.getClinicId(),
                        user.getProfessionalId()
                )
                .stream()
                .map(this::response)
                .toList();
    }

    public List<CscProviderResponse> providers() {
        return providerCatalog.providers().stream()
                .map(provider -> new CscProviderResponse(provider.key(), provider.name()))
                .toList();
    }

    @Transactional
    public CredentialResponse uploadLocal(
            MultipartFile file,
            String password,
            String displayName,
            boolean ownershipConfirmed
    ) {
        cryptoService.requireConfigured();
        var user = professionalUser();
        if (!ownershipConfirmed) {
            throw new IllegalArgumentException("Confirme a titularidade do certificado");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um certificado A1 .pfx ou .p12");
        }
        if (file.getSize() > MAX_PKCS12_BYTES) {
            throw new IllegalArgumentException("O certificado deve ter no máximo 2 MB");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Informe a senha do certificado");
        }
        try {
            var bytes = file.getBytes();
            var loaded = certificateMaterialService.loadPkcs12(bytes, password.toCharArray());
            assertNotDuplicated(user, loaded.metadata().fingerprintSha256());
            var encrypted = cryptoService.encrypt(bytes, context(user, SignatureMode.LOCAL_PKCS12));
            var credential = baseCredential(
                    user,
                    SignatureMode.LOCAL_PKCS12,
                    null,
                    displayName,
                    ownershipConfirmed,
                    loaded.metadata()
            );
            credential.setEncryptedMaterial(encrypted.content());
            credential.setEncryptionIv(encrypted.iv());
            return response(repository.save(credential));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível armazenar o certificado", exception);
        }
    }

    @Transactional
    public CredentialResponse connectRemote(RemoteCredentialRequest request) {
        cryptoService.requireConfigured();
        var user = professionalUser();
        var remote = remoteClient.credentialInfo(
                request.providerKey(),
                request.accessToken(),
                request.credentialId()
        );
        var material = new RemoteCredentialMaterial(
                request.accessToken(),
                remote.certificateChain().stream()
                        .map(Base64.getEncoder()::encodeToString)
                        .toList(),
                remote.metadata().signatureAlgorithm()
        );
        try {
            var encrypted = cryptoService.encrypt(
                    objectMapper.writeValueAsBytes(material),
                    context(user, SignatureMode.REMOTE_CSC)
            );
            var credential = repository
                    .findByClinicIdAndProfessionalIdAndFingerprintSha256(
                            user.getClinicId(),
                            user.getProfessionalId(),
                            remote.metadata().fingerprintSha256()
                    )
                    .map(existing -> {
                        if (existing.getMode() != SignatureMode.REMOTE_CSC) {
                            throw new IllegalArgumentException(
                                    "Este certificado já foi cadastrado em outra modalidade"
                            );
                        }
                        existing.setProviderKey(request.providerKey());
                        existing.setDisplayName(request.displayName().trim());
                        existing.setActive(true);
                        return existing;
                    })
                    .orElseGet(() -> baseCredential(
                            user,
                            SignatureMode.REMOTE_CSC,
                            request.providerKey(),
                            request.displayName(),
                            request.ownershipConfirmed(),
                            remote.metadata()
                    ));
            credential.setRemoteCredentialId(request.credentialId());
            credential.setRemoteSecretKind(remote.secretKind());
            credential.setEncryptedMaterial(encrypted.content());
            credential.setEncryptionIv(encrypted.iv());
            return response(repository.save(credential));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger a credencial remota", exception);
        }
    }

    @Transactional
    public void deactivate(String id) {
        var user = professionalUser();
        var credential = repository.findByIdAndClinicIdAndProfessionalId(
                        id,
                        user.getClinicId(),
                        user.getProfessionalId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Credencial não encontrada"));
        credential.setActive(false);
        repository.save(credential);
    }

    public SignatureCredential requireOwnedActive(String id, AppUser user) {
        var credential = repository.findByIdAndClinicIdAndProfessionalId(
                        id,
                        user.getClinicId(),
                        user.getProfessionalId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Credencial de assinatura não encontrada"));
        if (!credential.isActive()) {
            throw new IllegalStateException("Esta credencial está inativa");
        }
        var now = Instant.now();
        if (now.isBefore(credential.getValidFrom())) {
            throw new IllegalStateException("O certificado ainda não está válido");
        }
        if (now.isAfter(credential.getValidUntil())) {
            throw new IllegalStateException("O certificado está vencido");
        }
        return credential;
    }

    public byte[] decryptLocal(SignatureCredential credential) {
        return cryptoService.decrypt(
                credential.getEncryptedMaterial(),
                credential.getEncryptionIv(),
                context(credential)
        );
    }

    public RemoteCredentialMaterial decryptRemote(SignatureCredential credential) {
        try {
            var clear = cryptoService.decrypt(
                    credential.getEncryptedMaterial(),
                    credential.getEncryptionIv(),
                    context(credential)
            );
            return objectMapper.readValue(clear, RemoteCredentialMaterial.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível abrir a credencial remota", exception);
        }
    }

    private SignatureCredential baseCredential(
            AppUser user,
            SignatureMode mode,
            String providerKey,
            String displayName,
            boolean ownershipConfirmed,
            CertificateMaterialService.CertificateMetadata metadata
    ) {
        if (displayName != null && displayName.trim().length() > 160) {
            throw new IllegalArgumentException("O nome da credencial deve ter no máximo 160 caracteres");
        }
        var credential = new SignatureCredential();
        credential.setClinicId(user.getClinicId());
        credential.setProfessionalId(user.getProfessionalId());
        credential.setMode(mode);
        credential.setProviderKey(providerKey);
        credential.setDisplayName(displayName == null || displayName.isBlank()
                ? metadata.subjectName()
                : displayName.trim());
        credential.setSubjectName(metadata.subjectName());
        credential.setIssuerName(metadata.issuerName());
        credential.setSerialNumber(metadata.serialNumber());
        credential.setFingerprintSha256(metadata.fingerprintSha256());
        credential.setValidFrom(metadata.validFrom());
        credential.setValidUntil(metadata.validUntil());
        credential.setOwnershipConfirmed(ownershipConfirmed);
        credential.setCreatedByUserId(user.getId());
        return credential;
    }

    private void assertNotDuplicated(AppUser user, String fingerprint) {
        if (repository.existsByClinicIdAndProfessionalIdAndFingerprintSha256(
                user.getClinicId(),
                user.getProfessionalId(),
                fingerprint
        )) {
            throw new IllegalArgumentException("Este certificado já foi cadastrado");
        }
    }

    private AppUser professionalUser() {
        var user = accessService.currentUser();
        if (user.getProfessionalId() == null || user.getProfessionalId().isBlank()) {
            throw new IllegalStateException(
                    "O usuário precisa estar vinculado a um profissional para gerenciar certificados"
            );
        }
        return user;
    }

    private String context(AppUser user, SignatureMode mode) {
        return user.getClinicId() + ":" + user.getProfessionalId() + ":" + mode.name();
    }

    private String context(SignatureCredential credential) {
        return credential.getClinicId() + ":" + credential.getProfessionalId() + ":" + credential.getMode().name();
    }

    private CredentialResponse response(SignatureCredential credential) {
        var providerName = providerName(credential);
        return new CredentialResponse(
                credential.getId(),
                credential.getMode(),
                credential.getProviderKey(),
                providerName,
                credential.getDisplayName(),
                credential.getSubjectName(),
                credential.getIssuerName(),
                credential.getSerialNumber(),
                credential.getFingerprintSha256(),
                credential.getValidFrom(),
                credential.getValidUntil(),
                credential.getRemoteSecretKind(),
                credential.isActive(),
                credential.isOwnershipConfirmed(),
                credential.getLastUsedAt()
        );
    }

    private String providerName(SignatureCredential credential) {
        if (credential.getProviderKey() == null) return "Certificado A1";
        try {
            return providerCatalog.require(credential.getProviderKey()).name();
        } catch (RuntimeException ignored) {
            return credential.getProviderKey() + " (não configurado)";
        }
    }

    public record RemoteCredentialMaterial(
            String accessToken,
            List<String> certificateChainBase64,
            String signatureAlgorithm
    ) {
    }
}
