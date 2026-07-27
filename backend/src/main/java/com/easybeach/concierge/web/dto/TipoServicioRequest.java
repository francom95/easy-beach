package com.easybeach.concierge.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TipoServicioRequest(@NotBlank String nombre, boolean activo, int orden) {
}
