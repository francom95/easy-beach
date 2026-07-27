package com.easybeach.promotions;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato de cálculo de descuentos que {@code ordering} consume al crear un
 * pedido (ADR-002 permite {@code ordering -> promotions}, así que acá no hace
 * falta invertir la dependencia como con {@code ConsumoEstadiaProvider}).
 *
 * <p>La etapa 13 deja el contrato listo y testeado; la etapa 14 aporta la
 * implementación real (descuento %, combo, happy hour) reemplazando el bean
 * por defecto {@link SinPromocionesCalculadora}.
 */
public interface CalculadoraPromociones {

    /**
     * @param lineas ítems ya valorizados con precios de servidor.
     * @return descuentos a aplicar; lista vacía si no corresponde ninguno.
     */
    List<DescuentoAplicado> calcular(Long balnearioId, List<LineaPedido> lineas);

    /** Ítem valorizado por el servidor - la calculadora nunca ve precios del cliente. */
    record LineaPedido(Long productoId, Long categoriaId, Long productoVarianteId,
                        BigDecimal precioUnitario, int cantidad) {

        public BigDecimal subtotal() {
            return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    /** {@code promocionId} nulo es válido (promoción sintética/de prueba). */
    record DescuentoAplicado(Long promocionId, String nombre, BigDecimal monto) {
    }
}
