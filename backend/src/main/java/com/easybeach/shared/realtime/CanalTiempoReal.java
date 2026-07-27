package com.easybeach.shared.realtime;

/**
 * Los dos canales SSE de ADR-003. Cada uno se suscribe con una clave
 * distinta: el cliente por su {@code usuarioPublicId}, el staff por el
 * {@code balnearioId} de su token.
 */
public enum CanalTiempoReal {

    /** Eventos dirigidos a UN cliente: resultado del pago, estado de su pedido. */
    CLIENTE,

    /** Eventos del panel operativo de UN balneario: pedido nuevo, solicitud nueva. */
    OPERATIVO
}
