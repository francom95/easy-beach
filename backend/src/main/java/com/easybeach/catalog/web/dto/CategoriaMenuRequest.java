package com.easybeach.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaMenuRequest(
        @NotBlank @Size(max = 80) String nombre,
        int orden,
        boolean activa
) {
}
