package com.easybeach.platform.web.dto;

/**
 * {@code passwordTemporalAdmin} viaja UNA sola vez, en esta respuesta: sin
 * servicio de email todavía (fuera de alcance de la etapa 10), es la única
 * forma de que el Super Admin se la pueda pasar al admin del balneario. No
 * se persiste en claro (se hashea igual que cualquier password) ni se
 * vuelve a exponer en ningún otro endpoint.
 */
public record CrearBalnearioResponse(
        BalnearioResponse balneario,
        String emailAdmin,
        String passwordTemporalAdmin
) {
}
