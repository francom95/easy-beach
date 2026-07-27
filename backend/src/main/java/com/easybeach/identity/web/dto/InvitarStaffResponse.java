package com.easybeach.identity.web.dto;

/**
 * {@code passwordTemporal} viaja en la respuesta (no hay envío de email en
 * el MVP) - mismo patrón ya usado para el alta del admin de balneario
 * (etapa 10, {@code CrearBalnearioResponse}).
 */
public record InvitarStaffResponse(String usuarioPublicId, String email, String nombre, String rol, String passwordTemporal) {
}
