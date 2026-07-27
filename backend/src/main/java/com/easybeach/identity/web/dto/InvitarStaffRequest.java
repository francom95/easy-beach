package com.easybeach.identity.web.dto;

import com.easybeach.shared.security.RolCodigo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Solo CARPERO/OPERADOR (etapa 17): invitar otro ADMIN_BALNEARIO no es parte del MVP. */
public record InvitarStaffRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 120) String nombre,
        @NotNull RolCodigo rol
) {
}
