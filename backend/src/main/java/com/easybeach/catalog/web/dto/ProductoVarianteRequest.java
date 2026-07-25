package com.easybeach.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoVarianteRequest(
        @NotBlank @Size(max = 80) String nombre,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal precio,
        boolean disponible,
        int orden
) {
}
