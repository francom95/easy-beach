package com.easybeach.platform.web.dto;

import java.time.Instant;

public record AuditoriaResponse(
        Long id,
        Long actorUsuarioId,
        String accion,
        String entidadTipo,
        Long entidadId,
        Long balnearioId,
        Instant createdAt
) {
}
