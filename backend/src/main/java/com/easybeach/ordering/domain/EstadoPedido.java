package com.easybeach.ordering.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados del pedido (etapa 03 §4.2).
 *
 * <pre>
 * [*] -> CREADO                            (cliente confirma el carrito)
 *        CREADO         -> PAGO_PENDIENTE  (se crea el pago en MP)
 *        CREADO         -> CANCELADO       (cliente cancela, aún sin pagar)
 *        PAGO_PENDIENTE -> CONFIRMADO      (webhook MP: aprobado)
 *        PAGO_PENDIENTE -> PAGO_RECHAZADO  (webhook MP: rechazado)
 *        PAGO_PENDIENTE -> CANCELADO       (cliente cancela / timeout)
 *        PAGO_RECHAZADO -> PAGO_PENDIENTE  (reintento de pago)
 *        PAGO_RECHAZADO -> CANCELADO       (cliente desiste)
 *        CONFIRMADO     -> EN_PREPARACION  (operador toma)
 *        EN_PREPARACION -> EN_CAMINO       (operador despacha)
 *        EN_CAMINO      -> ENTREGADO       (operador entrega)
 *        CONFIRMADO | EN_PREPARACION -> CANCELADO (local cancela -> reembolso)
 * </pre>
 *
 * <p><b>Invariante central:</b> un pedido no entra a la cola operativa hasta
 * que su pago está aprobado ({@link #entroACola()}). Sin plata confirmada, la
 * cocina no ve nada.
 */
public enum EstadoPedido {

    CREADO,
    PAGO_PENDIENTE,
    PAGO_RECHAZADO,
    CONFIRMADO,
    EN_PREPARACION,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO;

    /** Estados en los que el pedido ya es visible y accionable para el staff. */
    private static final Set<EstadoPedido> EN_COLA_OPERATIVA =
            EnumSet.of(CONFIRMADO, EN_PREPARACION, EN_CAMINO);

    /** "En curso" desde la óptica de la estadía: bloquean su cierre (etapa 12). */
    private static final Set<EstadoPedido> EN_CURSO =
            EnumSet.of(CREADO, PAGO_PENDIENTE, PAGO_RECHAZADO, CONFIRMADO, EN_PREPARACION, EN_CAMINO);

    /** Estados donde ya hubo cobro efectivo: cancelar exige reembolso. */
    private static final Set<EstadoPedido> CON_PAGO_APROBADO =
            EnumSet.of(CONFIRMADO, EN_PREPARACION, EN_CAMINO, ENTREGADO);

    public boolean entroACola() {
        return EN_COLA_OPERATIVA.contains(this);
    }

    public boolean estaEnCurso() {
        return EN_CURSO.contains(this);
    }

    public boolean tienePagoAprobado() {
        return CON_PAGO_APROBADO.contains(this);
    }

    public boolean esTerminal() {
        return this == ENTREGADO || this == CANCELADO;
    }

    /** El cliente solo puede cancelar antes de que la cocina se ponga a trabajar. */
    public boolean cancelablePorCliente() {
        return this == CREADO || this == PAGO_PENDIENTE || this == PAGO_RECHAZADO;
    }

    /** El local puede cancelar mientras no esté entregado (con motivo y reembolso si corresponde). */
    public boolean cancelablePorLocal() {
        return !esTerminal();
    }

    public boolean puedeTransicionarA(EstadoPedido destino) {
        return switch (this) {
            case CREADO -> destino == PAGO_PENDIENTE || destino == CANCELADO;
            case PAGO_PENDIENTE -> destino == CONFIRMADO || destino == PAGO_RECHAZADO || destino == CANCELADO;
            case PAGO_RECHAZADO -> destino == PAGO_PENDIENTE || destino == CANCELADO;
            case CONFIRMADO -> destino == EN_PREPARACION || destino == CANCELADO;
            case EN_PREPARACION -> destino == EN_CAMINO || destino == CANCELADO;
            case EN_CAMINO -> destino == ENTREGADO || destino == CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
