package com.easybeach.reporting.dto;

import java.math.BigDecimal;

public record VolumenPorBalnearioResponse(Long balnearioId, String balnearioNombre, long cantidadPedidos,
                                           BigDecimal facturacion) {
}
