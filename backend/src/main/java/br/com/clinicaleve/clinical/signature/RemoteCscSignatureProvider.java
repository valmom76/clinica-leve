package br.com.clinicaleve.clinical.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class RemoteCscSignatureProvider implements DigitalSignatureProvider {

    private final SignatureCredentialService credentialService;
    private final CertificateMaterialService certificateMaterialService;
    private final CscRemoteClient remoteClient;

    @Override
    public SignatureMode mode() {
        return SignatureMode.REMOTE_CSC;
    }

    @Override
    public SigningIdentity prepare(SignatureCredential credential, String secret, String secondarySecret) {
        var material = credentialService.decryptRemote(credential);
        var derChain = material.certificateChainBase64().stream()
                .map(Base64.getDecoder()::decode)
                .toList();
        var chain = certificateMaterialService.decodeDerChain(derChain);
        return new SigningIdentity(
                chain,
                material.signatureAlgorithm(),
                content -> {
                    try {
                        var digest = MessageDigest.getInstance("SHA-256").digest(content);
                        return remoteClient.signHash(
                                credential.getProviderKey(),
                                material.accessToken(),
                                credential.getRemoteCredentialId(),
                                credential.getRemoteSecretKind(),
                                secret,
                                material.signatureAlgorithm(),
                                secondarySecret,
                                digest
                        );
                    } catch (Exception exception) {
                        throw new IllegalStateException("Falha na assinatura pelo provedor remoto", exception);
                    }
                }
        );
    }
}
