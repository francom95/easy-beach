package com.easybeach.identity.web.dto;

public record MiembroResponse(String usuarioPublicId, String usuarioNombre, String rol, Long balnearioId) {
}
