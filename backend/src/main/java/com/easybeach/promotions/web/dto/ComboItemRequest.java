package com.easybeach.promotions.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ComboItemRequest(@NotNull Long productoId, @Positive int cantidad) {
}
