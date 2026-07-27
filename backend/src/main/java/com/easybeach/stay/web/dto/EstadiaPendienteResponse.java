package com.easybeach.stay.web.dto;

import java.time.Instant;

/**
 * {@code EstadiaResponse} + {@code clienteNombre} (etapa 17): la bandeja de
 * validación del panel operativo necesita mostrar el nombre del cliente
 * (mockup de etapa 08, "Marcos Iribarne · MI"), campo que
 * {@code EstadiaResponse} nunca expuso porque el cliente no necesita ver su
 * propio nombre reflejado - se deja como DTO aparte para no tocar el
 * contrato que ya consume la app mobile (etapa 16).
 */
public record EstadiaPendienteResponse(
        String publicId,
        Long balnearioId,
        Long ubicacionId,
        String ubicacionIdentificador,
        String clienteNombre,
        String estado,
        Instant fechaSolicitud
) {
}
