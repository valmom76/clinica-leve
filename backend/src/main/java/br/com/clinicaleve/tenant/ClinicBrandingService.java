package br.com.clinicaleve.tenant;

import br.com.clinicaleve.shared.TenantAccess;
import br.com.clinicaleve.tenant.ClinicBrandingDtos.BrandingResponse;
import br.com.clinicaleve.tenant.ClinicBrandingDtos.LogoContent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicBrandingService {
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024;

    private final ClinicRepository clinicRepository;

    @Value("${app.uploads.directory:./uploads}")
    private String configuredUploadDirectory;
    private Path uploadDirectory;

    @PostConstruct
    void prepareDirectory() throws IOException {
        uploadDirectory = Path.of(configuredUploadDirectory).toAbsolutePath().normalize();
        Files.createDirectories(uploadDirectory);
    }

    @Transactional(readOnly = true)
    public BrandingResponse publicBranding(String slug) {
        var clinic = clinicRepository.findBySlugAndActiveTrue(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clínica não encontrada"));
        return response(clinic);
    }

    @Transactional(readOnly = true)
    public BrandingResponse currentBranding() {
        return response(currentClinic());
    }

    @Transactional
    public BrandingResponse updateTheme(ClinicTheme themeKey) {
        var clinic = currentClinic();
        clinic.setThemeKey(themeKey);
        return response(clinicRepository.save(clinic));
    }

    @Transactional
    public BrandingResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione uma imagem para a logomarca");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("A logomarca deve ter no máximo 2 MB");
        }

        var format = detectFormat(readBytes(file));
        var clinic = currentClinic();
        var previousFile = clinic.getLogoFileName();
        var fileName = clinic.getId() + "-" + UUID.randomUUID() + "." + format.extension();
        var destination = safePath(fileName);

        try {
            Files.write(destination, file.getBytes(), StandardOpenOption.CREATE_NEW);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível armazenar a logomarca", exception);
        }

        clinic.setLogoFileName(fileName);
        clinic.setLogoContentType(format.contentType());
        var saved = clinicRepository.save(clinic);
        deleteQuietly(previousFile);
        return response(saved);
    }

    @Transactional
    public BrandingResponse remove() {
        var clinic = currentClinic();
        var previousFile = clinic.getLogoFileName();
        clinic.setLogoFileName(null);
        clinic.setLogoContentType(null);
        var saved = clinicRepository.save(clinic);
        deleteQuietly(previousFile);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public LogoContent logo(String slug) {
        var clinic = clinicRepository.findBySlugAndActiveTrue(slug.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (clinic.getLogoFileName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var path = safePath(clinic.getLogoFileName());
        try {
            return new LogoContent(
                    Files.readAllBytes(path),
                    clinic.getLogoContentType(),
                    clinic.getUpdatedAt() == null ? Instant.now().toEpochMilli() : clinic.getUpdatedAt().toEpochMilli()
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logomarca não encontrada");
        }
    }

    public static String logoUrl(Clinic clinic) {
        if (clinic.getLogoFileName() == null) return null;
        var version = clinic.getUpdatedAt() == null ? 0 : clinic.getUpdatedAt().toEpochMilli();
        return "/api/public/branding/" + clinic.getSlug() + "/logo?v=" + version;
    }

    private BrandingResponse response(Clinic clinic) {
        return new BrandingResponse(
                clinic.getName(),
                clinic.getSlug(),
                logoUrl(clinic),
                clinic.getThemeKey()
        );
    }

    private Clinic currentClinic() {
        return clinicRepository.findById(TenantAccess.currentClinicId())
                .orElseThrow(() -> new IllegalArgumentException("Clínica não encontrada"));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler a imagem", exception);
        }
    }

    private LogoFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return new LogoFormat("png", "image/png");
        }
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return new LogoFormat("jpg", "image/jpeg");
        }
        throw new IllegalArgumentException("Use uma imagem PNG ou JPG válida");
    }

    private Path safePath(String fileName) {
        var path = uploadDirectory.resolve(fileName).normalize();
        if (!path.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Nome de arquivo inválido");
        }
        return path;
    }

    private void deleteQuietly(String fileName) {
        if (fileName == null) return;
        try {
            Files.deleteIfExists(safePath(fileName));
        } catch (IOException ignored) {
            // O registro já aponta para o arquivo atual; a limpeza pode ser refeita operacionalmente.
        }
    }

    private record LogoFormat(String extension, String contentType) {}
}
