package com.easybeach.shared.tenancy;

/**
 * Se lanza cuando una operación tenant-scoped se ejecuta sin
 * {@link TenantContext} resuelto. Fail-fast intencional: una query sin
 * tenant debe FALLAR, nunca devolver datos de todos los balnearios
 * (criterio de aceptación de la etapa 09 / ADR-001).
 */
public class TenantContextMissingException extends RuntimeException {

    public TenantContextMissingException() {
        super("Operación tenant-scoped invocada sin TenantContext resuelto");
    }
}
