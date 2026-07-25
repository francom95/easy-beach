package com.easybeach.platform.web.dto;

public record BalnearioResponse(
        Long id,
        String slug,
        String nombre,
        String emailContacto,
        String telefono,
        String estado,
        boolean operativo
) {
}
