package com.easybeach.shared.error;

import org.springframework.http.HttpStatus;

/**
 * Códigos de negocio estables (etapa 04 §1.3). El {@code code} es contrato
 * con mobile/web; title/detail son legibles y pueden cambiar.
 */
public enum ErrorCode {

    VALIDACION_FALLIDA(HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos"),
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, "El token es inválido o expiró"),
    REFRESH_REUTILIZADO(HttpStatus.UNAUTHORIZED, "El refresh token ya fue usado; se revocó la sesión"),
    ACCESO_DENEGADO(HttpStatus.FORBIDDEN, "No tenés permiso para esta operación"),
    RECURSO_NO_ENCONTRADO(HttpStatus.NOT_FOUND, "El recurso solicitado no existe"),
    EMAIL_YA_REGISTRADO(HttpStatus.CONFLICT, "Ya existe una cuenta con ese email"),
    BALNEARIO_NO_OPERATIVO(HttpStatus.CONFLICT, "El balneario no está operativo en este momento"),
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
