package com.easybeach.payments;

import java.math.BigDecimal;

/**
 * Operaciones de cobro contra la API de Mercado Pago, siempre con el
 * {@code access_token} del balneario dueño del pedido y
 * {@code application_fee = 0} (ADR-004: marketplace sin comisión).
 *
 * <p>Igual que {@link MercadoPagoOAuthClient}, en tests se reemplaza por un
 * fake: no hay credenciales reales de una app de MP para golpear en CI.
 */
public interface MercadoPagoPaymentClient {

    /** Crea el pago. El {@code cardToken} lo generó el SDK de MP en el dispositivo (nunca vemos el PAN). */
    ResultadoPago crearPago(String accessTokenBalneario, String idempotencyKey,
                             BigDecimal monto, String descripcion, String cardToken);

    /**
     * Reconsulta server-to-server el estado real del pago. ADR-004: el body
     * del webhook <b>no es fuente de verdad</b>, esto sí.
     */
    ResultadoPago consultarPago(String accessTokenBalneario, String mpPaymentId);

    /** Cancelación por el local de un pedido ya cobrado. */
    ResultadoPago reembolsar(String accessTokenBalneario, String mpPaymentId);

    /** Estados de MP normalizados a nuestro dominio. */
    enum EstadoMp {
        APPROVED,
        REJECTED,
        PENDING,
        REFUNDED
    }

    record ResultadoPago(String mpPaymentId, EstadoMp estado, String statusDetail,
                          String metodo, BigDecimal monto) {
    }
}
