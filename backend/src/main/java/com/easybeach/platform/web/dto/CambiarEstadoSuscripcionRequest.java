package com.easybeach.platform.web.dto;

import com.easybeach.platform.domain.EstadoSuscripcion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoSuscripcionRequest(@NotNull EstadoSuscripcion estado, String motivo) {
}
