package com.easybeach.platform.event;

/**
 * Evento de dominio (etapa 02 §"eventos internos", ApplicationEventPublisher).
 * {@code branding} escucha este evento para sembrar el theme default -
 * `platform` no puede depender de `branding` (ADR-002: la flecha va al
 * revés), así que la relación se invierte con un evento en vez de una
 * llamada directa de {@code BalnearioService} a
 * {@code ConfiguracionVisualService}.
 */
public record BalnearioCreado(Long balnearioId) {
}
