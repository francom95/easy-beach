package com.easybeach.ordering.service;

import com.easybeach.payments.event.PagoResuelto;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * {@code ordering} reacciona al pago resuelto por {@code payments}. Es el
 * único punto donde un pedido pasa a {@code CONFIRMADO}: sin plata aprobada,
 * la cocina no ve nada.
 *
 * <p>{@code @EventListener} síncrono (no {@code AFTER_COMMIT}): la resolución
 * del pago y la transición del pedido deben ser atómicas - si la transición
 * falla, el pago tampoco se da por resuelto.
 */
@Component
public class PagoResueltoListener {

    private final PedidoService pedidoService;

    public PagoResueltoListener(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @EventListener
    public void onPagoResuelto(PagoResuelto evento) {
        pedidoService.aplicarResultadoDePago(evento.pedidoId(), evento.aprobado());
    }
}
