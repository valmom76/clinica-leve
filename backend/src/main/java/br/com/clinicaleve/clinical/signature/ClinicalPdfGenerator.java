package br.com.clinicaleve.clinical.signature;

import br.com.clinicaleve.clinical.ClinicalDocument;
import br.com.clinicaleve.patient.Patient;
import br.com.clinicaleve.professional.Professional;
import br.com.clinicaleve.tenant.Clinic;
import br.com.clinicaleve.tenant.ClinicBrandingService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ClinicalPdfGenerator {

    private static final float MARGIN = 52;
    private static final float BODY_SIZE = 10.5f;
    private static final float BODY_LEADING = 15;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));

    private final ClinicBrandingService brandingService;

    public byte[] generate(
            ClinicalDocument clinicalDocument,
            Clinic clinic,
            Patient patient,
            Professional professional,
            SignatureCredential credential,
            String verificationCode,
            Instant signingTime,
            String verificationUrl
    ) {
        try (var pdf = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            var logo = loadLogo(pdf, clinic);
            var writer = new PageWriter(pdf, regular, bold, clinic, logo);
            writer.newPage();

            writer.text(safe(clinicalDocument.getTitle()), bold, 16, 24);
            writer.text("Paciente: " + safe(patient.getName()), regular, 9, 16);
            writer.text(
                    "Profissional: " + safe(professional.getName())
                            + (professional.getCouncil() == null ? "" : " - " + safe(professional.getCouncil())),
                    regular,
                    9,
                    21
            );

            for (var line : wrappedBody(clinicalDocument.getContent(), regular)) {
                writer.ensureSpace(72);
                writer.text(line, regular, BODY_SIZE, BODY_LEADING);
            }

            writer.ensureSpace(132);
            writer.signatureBlock(
                    credential,
                    professional,
                    verificationCode,
                    DATE_TIME.format(signingTime.atZone(ZoneId.of(clinic.getTimezone()))),
                    verificationUrl,
                    qrCode(verificationUrl)
            );
            writer.closeCurrent();
            writer.addFooters();
            pdf.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF clínico", exception);
        }
    }

    private List<String> wrappedBody(String body, PDType1Font font) throws IOException {
        var result = new ArrayList<String>();
        for (var paragraph : safe(body).split("\\R", -1)) {
            if (paragraph.isBlank()) {
                result.add("");
                continue;
            }
            result.addAll(wrap(paragraph, font, BODY_SIZE, PDRectangle.A4.getWidth() - MARGIN * 2));
        }
        return result;
    }

    private List<String> wrap(String text, PDType1Font font, float size, float maxWidth) throws IOException {
        var lines = new ArrayList<String>();
        var current = new StringBuilder();
        for (var word : safe(text).split("\\s+")) {
            var candidate = current.isEmpty() ? word : current + " " + word;
            var width = font.getStringWidth(candidate) / 1000f * size;
            if (width > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private PDImageXObject loadLogo(PDDocument pdf, Clinic clinic) {
        try {
            var logo = brandingService.logo(clinic.getSlug());
            return PDImageXObject.createFromByteArray(pdf, logo.bytes(), "clinic-logo");
        } catch (RuntimeException | IOException ignored) {
            return null;
        }
    }

    private byte[] qrCode(String value) throws Exception {
        var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 220, 220);
        var image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (var x = 0; x < matrix.getWidth(); x++) {
            for (var y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }

    private String safe(String value) {
        if (value == null) return "";
        return value
                .replace('–', '-')
                .replace('—', '-')
                .replace('“', '"')
                .replace('”', '"')
                .replace('’', '\'');
    }

    private static final class PageWriter {
        private final PDDocument pdf;
        private final PDType1Font regular;
        private final PDType1Font bold;
        private final Clinic clinic;
        private final PDImageXObject logo;
        private PDPage currentPage;
        private PDPageContentStream stream;
        private float y;

        private PageWriter(
                PDDocument pdf,
                PDType1Font regular,
                PDType1Font bold,
                Clinic clinic,
                PDImageXObject logo
        ) {
            this.pdf = pdf;
            this.regular = regular;
            this.bold = bold;
            this.clinic = clinic;
            this.logo = logo;
        }

        private void newPage() throws IOException {
            closeCurrent();
            currentPage = new PDPage(PDRectangle.A4);
            pdf.addPage(currentPage);
            stream = new PDPageContentStream(pdf, currentPage);
            if (logo != null) {
                var scale = Math.min(92f / logo.getWidth(), 42f / logo.getHeight());
                stream.drawImage(logo, MARGIN, 770, logo.getWidth() * scale, logo.getHeight() * scale);
            }
            textAt(clinic.getName(), bold, 12, 154, 792);
            textAt("Documento clínico", regular, 8, 154, 777);
            stream.setStrokingColor(210, 220, 216);
            stream.moveTo(MARGIN, 758);
            stream.lineTo(PDRectangle.A4.getWidth() - MARGIN, 758);
            stream.stroke();
            y = 732;
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required < 52) {
                newPage();
            }
        }

        private void text(String text, PDType1Font font, float size, float leading) throws IOException {
            textAt(text, font, size, MARGIN, y);
            y -= leading;
        }

        private void textAt(
                String text,
                PDType1Font font,
                float size,
                float x,
                float positionY
        ) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(38, 57, 53);
            stream.newLineAtOffset(x, positionY);
            stream.showText(pdfText(text));
            stream.endText();
        }

        private String pdfText(String value) {
            if (value == null) return "";
            var encoder = Charset.forName("windows-1252").newEncoder();
            var output = new StringBuilder(value.length());
            value.codePoints().forEach(codePoint -> {
                var character = new String(Character.toChars(codePoint));
                output.append(encoder.canEncode(character) ? character : "?");
            });
            return output.toString();
        }

        private void signatureBlock(
                SignatureCredential credential,
                Professional professional,
                String verificationCode,
                String signedAt,
                String verificationUrl,
                byte[] qrBytes
        ) throws IOException {
            stream.setNonStrokingColor(241, 246, 243);
            stream.addRect(MARGIN, y - 104, PDRectangle.A4.getWidth() - MARGIN * 2, 104);
            stream.fill();
            textAt("ASSINADO DIGITALMENTE", bold, 10, MARGIN + 14, y - 20);
            textAt(professional.getName(), bold, 9.5f, MARGIN + 14, y - 38);
            textAt(professional.getCouncil() == null ? "" : professional.getCouncil(), regular, 8, MARGIN + 14, y - 53);
            textAt("Método: " + (credential.getMode() == SignatureMode.LOCAL_PKCS12
                    ? "Certificado A1"
                    : "Certificado em nuvem - " + credential.getProviderKey()), regular, 7.5f, MARGIN + 14, y - 68);
            textAt("Data: " + signedAt, regular, 7.5f, MARGIN + 14, y - 81);
            textAt("Código: " + verificationCode, regular, 7.5f, MARGIN + 14, y - 94);
            var qr = PDImageXObject.createFromByteArray(pdf, qrBytes, "verification-qr");
            stream.drawImage(qr, PDRectangle.A4.getWidth() - MARGIN - 88, y - 92, 78, 78);
            textAt("Valide em: " + verificationUrl, regular, 6.5f, MARGIN + 14, y - 114);
            y -= 124;
        }

        private void closeCurrent() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private void addFooters() throws IOException {
            var pages = pdf.getNumberOfPages();
            for (var index = 0; index < pages; index++) {
                var page = pdf.getPage(index);
                try (var footer = new PDPageContentStream(
                        pdf,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                )) {
                    footer.beginText();
                    footer.setFont(regular, 7);
                    footer.setNonStrokingColor(100, 116, 112);
                    footer.newLineAtOffset(MARGIN, 28);
                    footer.showText("Clínica Leve - " + clinic.getName() + " | Página " + (index + 1) + " de " + pages);
                    footer.endText();
                }
            }
        }
    }
}
