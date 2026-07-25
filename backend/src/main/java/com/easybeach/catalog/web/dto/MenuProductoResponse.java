package com.easybeach.catalog.web.dto;

import java.math.BigDecimal;
import java.util.List;

/** {@code precioBase} es el precio a mostrar solo si {@code variantes} está vacío (etapa 03 §3.4). */
public record MenuProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        String fotoUrl,
        List<MenuVarianteResponse> variantes
) {
}
