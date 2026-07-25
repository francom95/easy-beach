package com.easybeach.stay.web.dto;

import com.easybeach.stay.domain.EstadoUbicacion;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUbicacionRequest(@NotNull EstadoUbicacion estado) {
}
