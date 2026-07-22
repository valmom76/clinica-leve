package br.com.clinicaleve.clinical.signature;

import java.security.cert.X509Certificate;
import java.util.List;

public interface DigitalSignatureProvider {

    SignatureMode mode();

    SigningIdentity prepare(SignatureCredential credential, String secret, String secondarySecret);

    record SigningIdentity(
            List<X509Certificate> certificateChain,
            String signatureAlgorithm,
            RawSigner signer
    ) {
    }

    @FunctionalInterface
    interface RawSigner {
        byte[] sign(byte[] contentToSign);
    }
}
