package com.easybeach.stay.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * El cliente elige balneario (por slug, como en toda la navegación pública)
 * y su ubicación física. El {@code balnearioId} NUNCA viaja crudo desde el
 * cliente: se resuelve por slug en el servidor (ADR-001).
 */
public record SolicitarEstadiaRequest(
        @NotBlank String balnearioSlug,
        @NotNull Long ubicacionId
) {
}
