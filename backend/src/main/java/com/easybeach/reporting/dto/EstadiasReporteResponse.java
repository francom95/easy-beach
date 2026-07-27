package com.easybeach.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code aperturasPorDia}: estadías solicitadas cada día (por
 * {@code fecha_solicitud}), sin importar su desenlace. {@code
 * duracionPromedioHoras}/{@code consumoPromedioPorEstadia}: solo sobre
 * estadías CERRADAS o CERRADAS_POR_SISTEMA dentro del rango (por {@code
 * fecha_cierre}) - una estadía todavía abierta no tiene duración final.
 * {@code consumoPromedioPorEstadia} cuenta solo pedidos ENTREGADO, mismo
 * criterio que el resumen de cierre de la etapa 12.
 */
public record EstadiasReporteResponse(
        List<AperturasPorDiaResponse> aperturasPorDia,
        Double duracionPromedioHoras,
        BigDecimal consumoPromedioPorEstadia
) {
}
