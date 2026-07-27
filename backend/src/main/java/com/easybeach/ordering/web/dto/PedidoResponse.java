package com.easybeach.ordering.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
        String publicId,
        Long balnearioId,
        String ubicacionIdentificador,
        String estado,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal total,
        List<ItemResponse> items,
        List<PromocionAplicadaResponse> promociones,
        String motivoCancelacion,
        Instant createdAt
) {

    public record ItemResponse(String nombreProducto, String nombreVariante,
                                BigDecimal precioUnitario, int cantidad, BigDecimal subtotalLinea) {
    }

    public record PromocionAplicadaResponse(String nombre, BigDecimal montoDescuento) {
    }
}
