package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarBalnearioRequest(
        @NotBlank String nombre,
        @NotBlank @Email String emailContacto,
        String telefono
) {
}
