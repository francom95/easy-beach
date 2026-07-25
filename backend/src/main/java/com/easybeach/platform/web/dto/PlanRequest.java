package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal precio,
        boolean activo
) {
}
