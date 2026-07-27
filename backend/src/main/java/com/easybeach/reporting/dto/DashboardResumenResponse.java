package com.easybeach.reporting.dto;

import java.math.BigDecimal;

/**
 * KPIs del día para el dashboard del admin (etapa 08). "Día" = hoy en TZ de
 * negocio (etapa 04), no UTC. {@code facturacionHoy}/{@code pedidosHoy}
 * cuentan solo pedidos {@code ENTREGADO} (mismo criterio que el reporte de
 * ventas: los montos cuadran con lo entregado). {@code pedidosEnCurso} es
 * una foto del presente (no filtra por fecha): un pedido de ayer que sigue
 * en preparación cuenta igual.
 */
public record DashboardResumenResponse(
        BigDecimal facturacionHoy,
        long pedidosHoy,
        BigDecimal ticketPromedioHoy,
        long pedidosEnCurso
) {
}
