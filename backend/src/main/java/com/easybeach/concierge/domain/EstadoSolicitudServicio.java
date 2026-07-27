package com.easybeach.concierge.domain;

/** Ciclo simple (etapa 03 §3.8/§4.3), sin dinero de por medio. */
public enum EstadoSolicitudServicio {

    PENDIENTE,
    EN_CURSO,
    RESUELTA,
    CANCELADA;

    public boolean esTerminal() {
        return this == RESUELTA || this == CANCELADA;
    }

    public boolean puedeTransicionarA(EstadoSolicitudServicio destino) {
        return switch (this) {
            case PENDIENTE -> destino == EN_CURSO || destino == CANCELADA;
            case EN_CURSO -> destino == RESUELTA || destino == CANCELADA;
            case RESUELTA, CANCELADA -> false;
        };
    }
}
