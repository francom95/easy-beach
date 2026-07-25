package com.easybeach.shared.error;

/** Excepción de negocio con {@link ErrorCode} estable (etapa 04 §1.3). */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.defaultDetail());
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
