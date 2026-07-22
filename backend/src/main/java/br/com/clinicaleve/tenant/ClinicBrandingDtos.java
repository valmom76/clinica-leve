package br.com.clinicaleve.tenant;

import jakarta.validation.constraints.NotNull;

public final class ClinicBrandingDtos {
    private ClinicBrandingDtos() {}

    public record BrandingResponse(
            String clinicName,
            String clinicSlug,
            String logoUrl,
            ClinicTheme themeKey
    ) {}

    public record ThemeRequest(
            @NotNull ClinicTheme themeKey
    ) {}

    public record LogoContent(
            byte[] bytes,
            String contentType,
            long lastModified
    ) {}
}
