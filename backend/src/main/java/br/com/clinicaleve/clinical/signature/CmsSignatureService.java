package br.com.clinicaleve.clinical.signature;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.DefaultSignedAttributeTableGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

@Service
public class CmsSignatureService {

    public byte[] sign(byte[] content, DigitalSignatureProvider.SigningIdentity identity) {
        try {
            var certificate = identity.certificateChain().getFirst();
            var certificateHash = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            var essCert = new ESSCertIDv2(
                    new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256),
                    certificateHash
            );
            var attributes = new ASN1EncodableVector();
            attributes.add(new Attribute(
                    PKCSObjectIdentifiers.id_aa_signingCertificateV2,
                    new DERSet(new SigningCertificateV2(essCert))
            ));

            var digestProvider = new JcaDigestCalculatorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build();
            var signerInfo = new JcaSignerInfoGeneratorBuilder(digestProvider)
                    .setSignedAttributeGenerator(
                            new DefaultSignedAttributeTableGenerator(new AttributeTable(attributes))
                    )
                    .build(contentSigner(identity), certificate);

            var generator = new CMSSignedDataGenerator();
            generator.addSignerInfoGenerator(signerInfo);
            generator.addCertificates(new JcaCertStore(identity.certificateChain()));
            return generator.generate(new CMSProcessableByteArray(content), false).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível montar a assinatura PAdES", exception);
        }
    }

    private ContentSigner contentSigner(DigitalSignatureProvider.SigningIdentity identity) {
        return new ContentSigner() {
            private final ByteArrayOutputStream content = new ByteArrayOutputStream();

            @Override
            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return new DefaultSignatureAlgorithmIdentifierFinder().find(identity.signatureAlgorithm());
            }

            @Override
            public OutputStream getOutputStream() {
                content.reset();
                return content;
            }

            @Override
            public byte[] getSignature() {
                return identity.signer().sign(content.toByteArray());
            }
        };
    }
}
