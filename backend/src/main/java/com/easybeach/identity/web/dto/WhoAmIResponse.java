package com.easybeach.identity.web.dto;

public record WhoAmIResponse(String usuarioPublicId, String tipo, String rol, Long balnearioId) {
}
