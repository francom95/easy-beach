package com.easybeach.promotions.web.dto;

import com.easybeach.promotions.domain.EstadoPromocion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoPromocionRequest(@NotNull EstadoPromocion estado) {
}
