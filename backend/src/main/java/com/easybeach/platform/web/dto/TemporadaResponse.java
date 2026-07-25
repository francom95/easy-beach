package com.easybeach.platform.web.dto;

import java.time.LocalDate;

public record TemporadaResponse(Long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, String estado) {
}
