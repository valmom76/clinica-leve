package br.com.clinicaleve.clinical.signature;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SignedPdfStorage {

    private final SignatureProperties properties;
    private Path root;

    @PostConstruct
    void prepare() throws IOException {
        root = Path.of(properties.storageDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredPdf save(String clinicId, String documentId, String signatureId, byte[] content) {
        var relative = Path.of(clinicId, documentId + "-" + signatureId + ".pdf");
        var destination = safe(relative.toString());
        try {
            Files.createDirectories(destination.getParent());
            var temporary = Files.createTempFile(destination.getParent(), "signed-", ".tmp");
            Files.write(temporary, content);
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredPdf(relative.toString().replace('\\', '/'), hash(content));
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível armazenar o PDF assinado", exception);
        }
    }

    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(safe(relativePath));
        } catch (IOException exception) {
            throw new IllegalArgumentException("PDF assinado não encontrado", exception);
        }
    }

    public String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private Path safe(String relativePath) {
        var path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Caminho de assinatura inválido");
        }
        return path;
    }

    public record StoredPdf(String relativePath, String sha256) {
    }
}
