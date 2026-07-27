package com.easybeach.payments.event;

import java.math.BigDecimal;

/**
 * Publicado por {@code payments} cuando el pago de un pedido queda resuelto
 * (aprobado o rechazado). {@code ordering} lo escucha para mover el pedido.
 *
 * <p><b>Por qué un evento y no una llamada directa:</b> ADR-002 fija la
 * dirección {@code ordering -> payments}. El webhook vive en {@code payments}
 * y necesita empujar un cambio hacia {@code ordering}, o sea contra la
 * flecha. El evento invierte el sentido del acoplamiento sin violar la regla
 * (así estaba previsto en la tabla de eventos de la etapa 02 §2).
 */
public record PagoResuelto(Long pedidoId, Long balnearioId, boolean aprobado,
                            String mpPaymentId, BigDecimal monto, String statusDetail) {
}
