package com.easybeach.shared.tenancy;

/**
 * Contexto de tenant resuelto por request (ADR-001 / etapa 05 §3, capa 1).
 * Para staff, el valor sale exclusivamente del claim {@code balneario_id} del
 * JWT (ver {@link com.easybeach.identity.security.JwtAuthenticationFilter}).
 * Para cliente, el tenant se resuelve por recurso validado en servicio (no se
 * setea acá) - ver notas de la etapa 12/13.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long balnearioId) {
        CURRENT.set(balnearioId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
