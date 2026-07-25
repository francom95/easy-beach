package com.easybeach.shared.security;

/**
 * En {@code shared} (no {@code identity}): es un claim del JWT y un
 * concepto que cualquier módulo necesita para saber "quién llama" vía
 * {@link EasyBeachUserPrincipal} - {@code shared} no depende de ningún otro
 * módulo (ADR-002), así que este vocabulario vive acá y no en {@code identity}.
 */
public enum TipoUsuario {
    CLIENTE,
    STAFF,
    SUPER_ADMIN
}
