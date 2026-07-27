package com.easybeach.stay.web.dto;

import jakarta.validation.constraints.NotBlank;

/** El rechazo exige motivo: queda auditado junto al actor y el timestamp. */
public record RechazarEstadiaRequest(@NotBlank String motivo) {
}
