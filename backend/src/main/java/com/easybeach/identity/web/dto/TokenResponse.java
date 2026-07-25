package com.easybeach.identity.web.dto;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        String tipo,
        String rol,
        Long balnearioId
) {
}
