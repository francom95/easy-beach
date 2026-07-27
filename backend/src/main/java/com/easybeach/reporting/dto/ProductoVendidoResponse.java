package com.easybeach.reporting.dto;

import java.math.BigDecimal;

/** Ordenado por unidades vendidas descendente. Solo pedidos ENTREGADO. */
public record ProductoVendidoResponse(Long productoId, String nombreProducto, long unidadesVendidas,
                                       BigDecimal facturacion) {
}
