package com.easybeach.identity.web.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        String tipo,
        String rol,
        Long balnearioId,
        /** Etapa 05 §1.1: staff creado con password temporal debe cambiarla en el primer login. */
        boolean debeCambiarPassword
) {
}
