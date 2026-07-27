package com.easybeach.concierge.web.dto;

import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import jakarta.validation.constraints.NotNull;

public record TransicionSolicitudRequest(@NotNull EstadoSolicitudServicio estado) {
}
