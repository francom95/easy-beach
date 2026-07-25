package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Auditoría de acciones sensibles (etapa 05 §7 #12): toda suspensión exige motivo. */
public record MotivoRequest(@NotBlank String motivo) {
}
