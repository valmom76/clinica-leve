package br.com.clinicaleve.clinical.signature;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

@Service
@RequiredArgsConstructor
public class PdfPadesSigner {

    private static final int SIGNATURE_RESERVE_BYTES = 96 * 1024;

    private final CmsSignatureService cmsSignatureService;

    public byte[] sign(
            byte[] unsignedPdf,
            String signerName,
            DigitalSignatureProvider.SigningIdentity identity
    ) {
        try (var document = Loader.loadPDF(unsignedPdf);
             var output = new ByteArrayOutputStream();
             var options = new SignatureOptions()) {
            var signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            signature.setName(signerName);
            signature.setReason("Documento clínico finalizado na Clínica Leve");
            signature.setLocation("Brasil");
            signature.setSignDate(Calendar.getInstance());
            options.setPreferredSignatureSize(SIGNATURE_RESERVE_BYTES);
            document.addSignature(signature, options);

            var external = document.saveIncrementalForExternalSigning(output);
            var content = external.getContent().readAllBytes();
            external.setSignature(cmsSignatureService.sign(content, identity));
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar o PDF", exception);
        }
    }
}
