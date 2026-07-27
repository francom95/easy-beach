package com.easybeach.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Etapa 15 criterio de aceptación: "los montos cuadran con los pedidos
 * entregados (excluyen cancelados)". {@code facturacionTotal},
 * {@code cantidadPedidos} y {@code ticketPromedio} cuentan SOLO pedidos en
 * estado {@code ENTREGADO} - un pedido cancelado o aún en curso no facturó.
 */
public record VentasReporteResponse(
        BigDecimal facturacionTotal,
        long cantidadPedidos,
        BigDecimal ticketPromedio,
        List<VentasPorDiaResponse> porDia
) {
}
