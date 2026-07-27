package com.easybeach.promotions.web.dto;

import com.easybeach.promotions.domain.TipoAlcance;
import jakarta.validation.constraints.NotNull;

public record AlcancePromocionRequest(@NotNull TipoAlcance tipoAlcance, @NotNull Long referenciaId) {
}
