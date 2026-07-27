package com.easybeach.stay.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resumen de consumo al cerrar la estadía (etapa 07: "refuerza percepción de
 * valor"). {@code cantidadPedidos}/{@code montoTotal} se calculan sobre los
 * pedidos entregados; hasta que exista el módulo {@code ordering} (etapa 13)
 * llegan en cero - ver {@code ResumenConsumoProvider}.
 */
public record ResumenCierreResponse(
        String publicId,
        String balnearioNombre,
        Instant fechaSolicitud,
        Instant fechaCierre,
        long diasDeEstadia,
        int cantidadPedidos,
        BigDecimal montoTotal
) {
}
