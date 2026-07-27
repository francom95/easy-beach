package com.easybeach.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <b>Inversión de dependencia (ADR-002)</b>, mismo patrón que
 * {@code ConsumoEstadiaProvider} (etapa 12): el menú público necesita
 * mostrar promociones vigentes, pero la flecha de ADR-002 va
 * {@code promotions -> catalog}, nunca al revés. La interfaz vive acá (en
 * el consumidor); la implementación real la aporta {@code promotions} en
 * la etapa 14. Hasta entonces rige {@link SinPromocionesPublicasProvider}
 * (sin promociones definidas, "ninguna vigente" es correcto, no un stub).
 */
public interface PromocionesPublicasProvider {

    /**
     * Resuelve en un solo batch (no N+1) qué promociones de descuento%/happy
     * hour aplican a cada producto del menú, por sí mismo o por su categoría.
     * Los combos no se embeben por producto (aplican a una combinación) -
     * ver {@link #listarVigentes}.
     */
    Map<Long, List<PromocionResumen>> resolverPorProducto(Long balnearioId, List<ProductoParaPromo> productos);

    /** Todas las promociones vigentes del balneario, incluidos combos - etapa 07 "sección de promociones". */
    List<PromocionResumen> listarVigentes(Long balnearioId);

    record ProductoParaPromo(Long productoId, Long categoriaId) {
    }

    record PromocionResumen(Long id, String nombre, String tipo, BigDecimal valor) {
    }
}
