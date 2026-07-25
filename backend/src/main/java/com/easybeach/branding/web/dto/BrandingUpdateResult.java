package com.easybeach.branding.web.dto;

import java.util.Map;

/**
 * {@code aplicado=false}: al menos un color no cumplía contraste y
 * {@code aceptarSugerencia} no vino en true - {@code ajustesPropuestos}
 * trae el tono más cercano que sí cumple para cada token que falló (tokens.md:
 * "el guardado exige aceptarlo o corregir"). {@code aplicado=true}: se
 * guardó; {@code tokens} es el theme completo resuelto.
 */
public record BrandingUpdateResult(
        boolean aplicado,
        Map<String, Object> tokens,
        Map<String, String> ajustesPropuestos
) {
}
