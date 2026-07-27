package com.easybeach.stay.event;

import com.easybeach.stay.domain.EstadoEstadia;
import java.time.Instant;

/**
 * Publicado al cerrar una estadía. {@code estadoFinal} distingue el cierre
 * normal del cliente ({@code CERRADA}) del administrativo por fin de
 * temporada/suspensión ({@code CERRADA_POR_SISTEMA}) - la diferencia importa
 * para los reportes de la etapa 15.
 */
public record EstadiaCerrada(Long estadiaId, Long balnearioId, Long clienteId,
                              EstadoEstadia estadoFinal, Instant momento) {
}
