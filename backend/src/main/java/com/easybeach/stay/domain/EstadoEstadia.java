package com.easybeach.stay.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Máquina de estados de la estadía (etapa 03 §4.1). Las transiciones válidas
 * viven acá, no dispersas en el service: una transición inválida es un error
 * de negocio explícito, no un {@code if} olvidado.
 *
 * <pre>
 * [*] -> PENDIENTE_VALIDACION  (cliente solicita)
 *        PENDIENTE_VALIDACION -> ACTIVA     (carpero valida)
 *        PENDIENTE_VALIDACION -> RECHAZADA  (carpero rechaza)
 *        PENDIENTE_VALIDACION -> EXPIRADA   (job TTL, 60 min sin validar)
 *        ACTIVA -> CERRADA                  (cliente cierra)
 *        ACTIVA -> CERRADA_POR_SISTEMA      (fin de temporada)
 * </pre>
 */
public enum EstadoEstadia {

    PENDIENTE_VALIDACION,
    ACTIVA,
    RECHAZADA,
    CERRADA,
    CERRADA_POR_SISTEMA,
    EXPIRADA;

    /**
     * Estados que ocupan el "cupo" de unicidad por cliente+balneario: mientras
     * la estadía está en uno de estos, {@code activa_uk = cliente_id} y el
     * cliente no puede abrir otra en el MISMO balneario (sí en otro).
     */
    private static final Set<EstadoEstadia> OCUPAN_CUPO =
            EnumSet.of(PENDIENTE_VALIDACION, ACTIVA);

    public boolean ocupaCupo() {
        return OCUPAN_CUPO.contains(this);
    }

    public boolean esTerminal() {
        return !ocupaCupo();
    }

    /** Solo una estadía ACTIVA habilita pedidos (etapa 12 criterio de aceptación). */
    public boolean permitePedidos() {
        return this == ACTIVA;
    }

    public boolean puedeTransicionarA(EstadoEstadia destino) {
        return switch (this) {
            case PENDIENTE_VALIDACION -> destino == ACTIVA || destino == RECHAZADA || destino == EXPIRADA;
            case ACTIVA -> destino == CERRADA || destino == CERRADA_POR_SISTEMA;
            case RECHAZADA, CERRADA, CERRADA_POR_SISTEMA, EXPIRADA -> false;
        };
    }
}
