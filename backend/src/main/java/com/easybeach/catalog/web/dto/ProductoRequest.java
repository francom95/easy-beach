package com.easybeach.catalog.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoRequest(
        @NotNull Long categoriaId,
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal precioBase,
        boolean disponible,
        int orden
) {
}
