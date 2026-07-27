package com.easybeach.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VentasPorDiaResponse(LocalDate dia, long cantidadPedidos, BigDecimal facturacion) {
}
