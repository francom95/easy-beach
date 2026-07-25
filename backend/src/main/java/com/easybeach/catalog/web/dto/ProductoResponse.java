package com.easybeach.catalog.web.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        Long categoriaId,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        String fotoUrl,
        boolean disponible,
        int orden
) {
}
