package com.easybeach.promotions;

import java.util.List;

/**
 * Vigente hasta la etapa 14: sin promociones definidas, "ningún descuento"
 * es la respuesta correcta - no un stub que miente.
 */
public class SinPromocionesCalculadora implements CalculadoraPromociones {

    @Override
    public List<DescuentoAplicado> calcular(Long balnearioId, List<LineaPedido> lineas) {
        return List.of();
    }
}
