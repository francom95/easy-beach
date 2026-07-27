package com.easybeach.catalog.web.dto;

import com.easybeach.catalog.service.PromocionesPublicasProvider.PromocionResumen;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code precioBase} es el precio a mostrar solo si {@code variantes} está
 * vacío (etapa 03 §3.4). {@code promociones}: descuento%/happy hour vigentes
 * para este producto o su categoría (etapa 14) - los combos no se embeben
 * acá, ver el endpoint de promociones.
 */
public record MenuProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precioBase,
        String fotoUrl,
        List<MenuVarianteResponse> variantes,
        List<PromocionResumen> promociones
) {
}
