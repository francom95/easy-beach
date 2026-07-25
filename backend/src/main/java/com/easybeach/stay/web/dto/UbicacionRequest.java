package com.easybeach.stay.web.dto;

import com.easybeach.stay.domain.TipoUbicacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UbicacionRequest(
        @NotNull TipoUbicacion tipo,
        @NotBlank @Size(max = 40) String identificador
) {
}
