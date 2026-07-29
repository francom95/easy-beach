package com.easybeach.support;

import com.easybeach.payments.MercadoPagoPaymentClient;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fake del cliente de pagos de MP: no hay credenciales reales de una app de
 * Mercado Pago para golpear en CI. Es el único límite externo mockeado de la
 * suite - todo lo demás corre contra MySQL real.
 *
 * <p>Permite controlar el desenlace desde el test
 * ({@link Control#rechazarProximoPago()}) y cuenta cuántos cobros se
 * dispararon, para poder verificar que un reintento idempotente NO cobra dos
 * veces.
 */
@TestConfiguration
public class FakeMercadoPagoPaymentClient {

    /** Handle que los tests inyectan para dirigir el comportamiento del fake. */
    public static class Control {

        private final AtomicInteger cobros = new AtomicInteger();
        private final AtomicInteger reembolsos = new AtomicInteger();
        private volatile boolean rechazarProximo = false;
        private volatile boolean dejarPendiente = false;
        private final Map<String, MercadoPagoPaymentClient.EstadoMp> estadoPorPago = new ConcurrentHashMap<>();
        private final Set<String> consultasQueFallan = ConcurrentHashMap.newKeySet();

        public void rechazarProximoPago() {
            this.rechazarProximo = true;
        }

        /** Simula el caso real: MP responde "pending" y resuelve después por webhook. */
        public void dejarProximoPendiente() {
            this.dejarPendiente = true;
        }

        /** Fija el estado que devolverá {@code consultarPago} (lo que el webhook va a leer). */
        public void definirEstadoDe(String mpPaymentId, MercadoPagoPaymentClient.EstadoMp estado) {
            estadoPorPago.put(mpPaymentId, estado);
        }

        /**
         * Hace que {@code consultarPago} tire una excepción para ese pago:
         * simula la cuenta de MP de un balneario caída o su token vencido.
         */
        public void fallarConsultaDe(String mpPaymentId) {
            consultasQueFallan.add(mpPaymentId);
        }

        public int cobrosRealizados() {
            return cobros.get();
        }

        public int reembolsosRealizados() {
            return reembolsos.get();
        }

        public void reset() {
            cobros.set(0);
            reembolsos.set(0);
            rechazarProximo = false;
            dejarPendiente = false;
            estadoPorPago.clear();
            consultasQueFallan.clear();
        }
    }

    @Bean
    public Control controlPagos() {
        return new Control();
    }

    @Bean
    @Primary
    public MercadoPagoPaymentClient mercadoPagoPaymentClient(Control control) {
        return new MercadoPagoPaymentClient() {

            @Override
            public ResultadoPago crearPago(String accessTokenBalneario, String idempotencyKey,
                                            BigDecimal monto, String descripcion, String cardToken) {
                int n = control.cobros.incrementAndGet();
                String pagoId = "fake-pay-" + n + "-" + System.nanoTime();
                EstadoMp estado;
                if (control.rechazarProximo) {
                    control.rechazarProximo = false;
                    estado = EstadoMp.REJECTED;
                } else if (control.dejarPendiente) {
                    control.dejarPendiente = false;
                    estado = EstadoMp.PENDING;
                } else {
                    estado = EstadoMp.APPROVED;
                }
                control.estadoPorPago.put(pagoId, estado);
                return new ResultadoPago(pagoId, estado, "fake_" + estado, "visa", monto);
            }

            @Override
            public ResultadoPago consultarPago(String accessTokenBalneario, String mpPaymentId) {
                if (control.consultasQueFallan.contains(mpPaymentId)) {
                    throw new IllegalStateException("Fake: Mercado Pago no responde para " + mpPaymentId);
                }
                EstadoMp estado = control.estadoPorPago.getOrDefault(mpPaymentId, EstadoMp.APPROVED);
                return new ResultadoPago(mpPaymentId, estado, "fake_" + estado, "visa", null);
            }

            @Override
            public ResultadoPago reembolsar(String accessTokenBalneario, String mpPaymentId) {
                control.reembolsos.incrementAndGet();
                control.estadoPorPago.put(mpPaymentId, EstadoMp.REFUNDED);
                return new ResultadoPago(mpPaymentId, EstadoMp.REFUNDED, "refunded", "visa", null);
            }
        };
    }
}
