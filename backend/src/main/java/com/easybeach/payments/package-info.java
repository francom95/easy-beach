/**
 * Módulo <b>payments</b> (ADR-004): OAuth por balneario con Mercado Pago,
 * creación de pago, webhook, reconciliación, reembolsos. Depende de
 * {@code platform} y {@code shared}. La vinculación OAuth (adelantada a la
 * etapa 10, porque ocurre en el onboarding del balneario) ya está
 * construida; el resto (creación de pago, webhook, pedido_pago) llega en
 * la etapa 13.
 */
package com.easybeach.payments;
