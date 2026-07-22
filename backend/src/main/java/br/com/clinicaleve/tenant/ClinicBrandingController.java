package br.com.clinicaleve.tenant;

import br.com.clinicaleve.tenant.ClinicBrandingDtos.BrandingResponse;
import br.com.clinicaleve.tenant.ClinicBrandingDtos.ThemeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class ClinicBrandingController {
    private final ClinicBrandingService service;

    @GetMapping("/api/public/branding/{slug}")
    BrandingResponse publicBranding(@PathVariable String slug) {
        return service.publicBranding(slug);
    }

    @GetMapping("/api/public/branding/{slug}/logo")
    ResponseEntity<byte[]> logo(@PathVariable String slug) {
        var logo = service.logo(slug);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(logo.contentType()))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .lastModified(logo.lastModified())
                .body(logo.bytes());
    }

    @GetMapping("/api/clinic/branding")
    @PreAuthorize("hasRole('ADMIN')")
    BrandingResponse currentBranding() {
        return service.currentBranding();
    }

    @PutMapping("/api/clinic/branding/theme")
    @PreAuthorize("hasRole('ADMIN')")
    BrandingResponse updateTheme(@Valid @RequestBody ThemeRequest request) {
        return service.updateTheme(request.themeKey());
    }

    @PostMapping(value = "/api/clinic/branding/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    BrandingResponse upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }

    @DeleteMapping("/api/clinic/branding/logo")
    @PreAuthorize("hasRole('ADMIN')")
    BrandingResponse remove() {
        return service.remove();
    }
}
