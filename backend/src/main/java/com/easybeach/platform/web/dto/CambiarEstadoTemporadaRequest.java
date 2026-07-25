package com.easybeach.platform.web.dto;

import com.easybeach.platform.domain.EstadoTemporada;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTemporadaRequest(@NotNull EstadoTemporada estado) {
}
