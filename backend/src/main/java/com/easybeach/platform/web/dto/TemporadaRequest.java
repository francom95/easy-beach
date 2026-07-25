package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TemporadaRequest(
        @NotBlank String nombre,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin
) {
}
