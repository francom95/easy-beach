package com.easybeach.shared.security;

import java.security.SecureRandom;

/**
 * Generador de contraseñas temporales para altas de staff hechas por un
 * admin (balneario o super admin) en vez de auto-registro (etapa 10 §3 /
 * etapa 17 §staff). Extraído de {@code BalnearioService} para no duplicar
 * el alfabeto (sin caracteres ambiguos: sin {@code 0/O}, {@code 1/l/I}) en
 * cada lugar que crea un usuario con password temporal.
 */
public final class PasswordTemporalGenerator {

    private static final String CARACTERES = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LONGITUD = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordTemporalGenerator() {
    }

    public static String generar() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(CARACTERES.charAt(RANDOM.nextInt(CARACTERES.length())));
        }
        return sb.toString();
    }
}
