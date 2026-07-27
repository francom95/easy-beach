package com.easybeach.concierge.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SolicitarServicioRequest(@NotBlank String estadiaPublicId, @NotNull Long tipoServicioId, String nota) {
}
