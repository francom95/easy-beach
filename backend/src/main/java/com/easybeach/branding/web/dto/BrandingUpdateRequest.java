package com.easybeach.branding.web.dto;

import com.easybeach.branding.theming.TypographyFamily;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solo los tokens personalizables (etapa 06: colores base, tipografía,
 * nombre). Los {@code on-*}/{@code muted}/{@code border} se derivan siempre
 * en el servidor - nunca llegan en el request. Los assets (imágenes) se
 * suben por separado ({@code POST /admin/branding/assets/{tipo}}).
 */
public record BrandingUpdateRequest(
        @NotBlank String themeName,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorPrimary,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorSecondary,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorBackground,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorSurface,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorSuccess,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorWarning,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorError,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Debe ser un color hex de 6 dígitos, ej. #C95100")
        String colorInfo,
        TypographyFamily typographyFamily,
        /** Si algún color no cumple contraste, aplica la sugerencia del servidor en vez de rechazar el guardado. */
        boolean aceptarSugerencia
) {
}
