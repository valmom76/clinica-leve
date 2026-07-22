package br.com.clinicaleve.clinical.signature;

import org.apache.pdfbox.Loader;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

@Service
public class PdfSignatureVerifier {

    public boolean verify(byte[] pdfBytes) {
        try (var pdf = Loader.loadPDF(pdfBytes)) {
            var signatures = pdf.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                return false;
            }
            var signature = signatures.getLast();
            var cms = new CMSSignedData(
                    new CMSProcessableByteArray(signature.getSignedContent(pdfBytes)),
                    signature.getContents(pdfBytes)
            );
            var signer = cms.getSignerInfos().getSigners().stream().findFirst().orElse(null);
            if (signer == null) {
                return false;
            }
            var matches = cms.getCertificates().getMatches(signer.getSID());
            if (matches.isEmpty()) {
                return false;
            }
            var certificate = matches.iterator().next();
            return signer.verify(new JcaSimpleSignerInfoVerifierBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build((X509CertificateHolder) certificate));
        } catch (Exception exception) {
            return false;
        }
    }
}
