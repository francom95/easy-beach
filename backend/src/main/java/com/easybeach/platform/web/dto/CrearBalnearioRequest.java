package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearBalnearioRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,60}$", message = "Solo minúsculas, números y guiones") String slug,
        @NotBlank String nombre,
        @NotBlank @Email String emailContactoBalneario,
        String telefono,
        @NotBlank String nombreAdmin,
        @NotBlank @Email String emailAdmin
) {
}
