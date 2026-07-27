package com.easybeach.stay.web.dto;

import java.time.Instant;

/**
 * {@code publicId} (ULID) es el identificador que ve el cliente - nunca el
 * id numérico (etapa 05, amenaza #1: enumeración de recursos ajenos).
 */
public record EstadiaResponse(
        String publicId,
        Long balnearioId,
        String balnearioNombre,
        Long ubicacionId,
        String ubicacionIdentificador,
        String estado,
        boolean permitePedidos,
        Instant fechaSolicitud,
        Instant fechaValidacion,
        Instant fechaCierre,
        String motivoRechazo
) {
}
