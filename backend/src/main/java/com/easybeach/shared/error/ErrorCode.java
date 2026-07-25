package com.easybeach.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Códigos de negocio estables (etapa 04 §1.3). El {@code code} es contrato
 * con mobile/web; title/detail son legibles y pueden cambiar.
 */
public enum ErrorCode {

    // Etapa 04 §1.2: "Datos inválidos ⇒ 422" (no 400) - corregido en la etapa 11
    // al notar la discrepancia; ver docs/especificacion/11-backend-catalogo-ubicaciones.md.
    VALIDACION_FALLIDA(HttpStatus.UNPROCESSABLE_ENTITY, "Uno o más campos no son válidos"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "El token es inválido o expiró"),
    REFRESH_REUTILIZADO(HttpStatus.UNAUTHORIZED, "El refresh token ya fue usado; se revocó la sesión"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tenés permiso para esta operación"),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El recurso solicitado no existe"),
    EMAIL_YA_REGISTRADO(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email"),
    BALNEARIO_NO_OPERATIVO(HttpStatus.CONFLICT, "El balneario no está operativo en este momento"),
    // Etapa 04 §1.2: "Conflicto de estado (idempotencia, unicidad, transición
    // inválida) ⇒ 409" - distinto de VALIDACION_FALLIDA (422, dato mal formado).
    // Introducido en la etapa 11; VALIDACION_FALLIDA en algunos usos de las
    // etapas 09/10 debería ser esto (ver nota en el entregable de la etapa 11).
    CONFLICTO_DE_ESTADO(HttpStatus.CONFLICT, "La operación entra en conflicto con el estado actual del recurso"),
    ERROR_INESPERADO(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");

    private final HttpStatus status;
    private final String defaultDetail;

    ErrorCode(HttpStatus status, String defaultDetail) {
        this.status = status;
        this.defaultDetail = defaultDetail;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultDetail() {
        return defaultDetail;
    }
}
