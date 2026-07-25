package com.easybeach.shared.security;

/**
 * Roles cerrados (etapa 05 §2). En {@code shared} por el mismo motivo que
 * {@link TipoUsuario}: es vocabulario de autorización usado por cualquier
 * módulo (p.ej. {@code @PreAuthorize("hasRole(...)")}), no una entidad de
 * negocio de {@code identity}.
 */
public enum RolCodigo {
    CLIENTE,
    CARPERO,
    OPERADOR,
    ADMIN_BALNEARIO,
    SUPER_ADMIN
}
