package br.com.clinicaleve.clinical.signature;

import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;

@Service
public class CertificateMaterialService {

    public LoadedPkcs12 loadPkcs12(byte[] pkcs12, char[] password) {
        try {
            var keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(pkcs12), password);
            var alias = privateKeyAlias(keyStore.aliases(), keyStore);
            if (alias == null) {
                throw new IllegalArgumentException("O arquivo não possui uma chave privada para assinatura");
            }
            var key = keyStore.getKey(alias, password);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new IllegalArgumentException("A chave privada do certificado não pôde ser carregada");
            }
            var chain = new ArrayList<X509Certificate>();
            for (var certificate : keyStore.getCertificateChain(alias)) {
                if (certificate instanceof X509Certificate x509Certificate) {
                    chain.add(x509Certificate);
                }
            }
            if (chain.isEmpty()) {
                throw new IllegalArgumentException("O certificado não possui uma cadeia X.509 válida");
            }
            var metadata = metadata(chain);
            validateForSigning(chain.get(0));
            return new LoadedPkcs12(privateKey, List.copyOf(chain), metadata);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Não foi possível abrir o certificado. Confira o arquivo e a senha.",
                    exception
            );
        }
    }

    public CertificateMetadata fromDerChain(List<byte[]> certificates) {
        try {
            var factory = CertificateFactory.getInstance("X.509");
            var chain = certificates.stream()
                    .map(bytes -> {
                        try {
                            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
                        } catch (Exception exception) {
                            throw new IllegalArgumentException("Cadeia retornada pelo provedor remoto é inválida", exception);
                        }
                    })
                    .toList();
            if (chain.isEmpty()) {
                throw new IllegalArgumentException("O provedor remoto não retornou o certificado");
            }
            validateForSigning(chain.get(0));
            return metadata(chain);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Certificado remoto inválido", exception);
        }
    }

    public List<X509Certificate> decodeDerChain(List<byte[]> certificates) {
        try {
            var factory = CertificateFactory.getInstance("X.509");
            var result = new ArrayList<X509Certificate>();
            for (var bytes : certificates) {
                result.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes)));
            }
            return List.copyOf(result);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cadeia de certificados inválida", exception);
        }
    }

    private String privateKeyAlias(Enumeration<String> aliases, KeyStore keyStore) throws Exception {
        while (aliases.hasMoreElements()) {
            var alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                return alias;
            }
        }
        return null;
    }

    private CertificateMetadata metadata(List<X509Certificate> chain) throws Exception {
        var certificate = chain.get(0);
        return new CertificateMetadata(
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                certificate.getSerialNumber().toString(16).toUpperCase(),
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded())
                ),
                certificate.getNotBefore().toInstant(),
                certificate.getNotAfter().toInstant(),
                chain.stream().map(item -> {
                    try {
                        return item.getEncoded();
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                }).toList(),
                signatureAlgorithm(certificate.getPublicKey().getAlgorithm())
        );
    }

    private void validateForSigning(X509Certificate certificate) throws Exception {
        certificate.checkValidity();
        var keyUsage = certificate.getKeyUsage();
        if (keyUsage != null && keyUsage.length > 0 && !keyUsage[0]) {
            throw new IllegalArgumentException("O certificado não permite assinatura digital");
        }
    }

    private String signatureAlgorithm(String keyAlgorithm) {
        return switch (keyAlgorithm.toUpperCase()) {
            case "RSA" -> "SHA256withRSA";
            case "EC", "ECDSA" -> "SHA256withECDSA";
            default -> throw new IllegalArgumentException(
                    "Algoritmo de chave não suportado: " + keyAlgorithm
            );
        };
    }

    public record CertificateMetadata(
            String subjectName,
            String issuerName,
            String serialNumber,
            String fingerprintSha256,
            Instant validFrom,
            Instant validUntil,
            List<byte[]> certificateChain,
            String signatureAlgorithm
    ) {
    }

    public record LoadedPkcs12(
            PrivateKey privateKey,
            List<X509Certificate> certificateChain,
            CertificateMetadata metadata
    ) {
    }
}
