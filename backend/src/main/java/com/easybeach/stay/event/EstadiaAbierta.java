package com.easybeach.stay.event;

import java.time.Instant;

/**
 * Publicado cuando un carpero valida la solicitud y la estadía pasa a
 * {@code ACTIVA} (etapa 12 §4). La etapa 15 (reportes) y futuras features
 * (ej. promo de bienvenida) se cuelgan de acá sin que {@code stay} tenga que
 * conocerlas.
 */
public record EstadiaAbierta(Long estadiaId, Long balnearioId, Long clienteId, Instant momento) {
}
