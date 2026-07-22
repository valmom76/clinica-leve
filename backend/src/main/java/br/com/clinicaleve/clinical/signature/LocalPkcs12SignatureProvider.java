package br.com.clinicaleve.clinical.signature;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.security.Signature;

@Component
@RequiredArgsConstructor
public class LocalPkcs12SignatureProvider implements DigitalSignatureProvider {

    private final SignatureCredentialService credentialService;
    private final CertificateMaterialService certificateMaterialService;

    @Override
    public SignatureMode mode() {
        return SignatureMode.LOCAL_PKCS12;
    }

    @Override
    public SigningIdentity prepare(SignatureCredential credential, String secret, String secondarySecret) {
        var pkcs12 = credentialService.decryptLocal(credential);
        var loaded = certificateMaterialService.loadPkcs12(pkcs12, secret.toCharArray());
        return new SigningIdentity(
                loaded.certificateChain(),
                loaded.metadata().signatureAlgorithm(),
                content -> {
                    try {
                        var signature = Signature.getInstance(
                                loaded.metadata().signatureAlgorithm(),
                                BouncyCastleProvider.PROVIDER_NAME
                        );
                        signature.initSign(loaded.privateKey());
                        signature.update(content);
                        return signature.sign();
                    } catch (Exception exception) {
                        throw new IllegalStateException("Falha ao usar a chave privada do certificado", exception);
                    }
                }
        );
    }
}
