package com.easybeach.reporting.dto;

import java.math.BigDecimal;

/** {@code nombrePromocion} es el nombre CONGELADO al momento del pedido (etapa 14), no el actual. */
public record PromocionRendimientoResponse(Long promocionId, String nombrePromocion, long usos,
                                            BigDecimal montoDescontado) {
}
