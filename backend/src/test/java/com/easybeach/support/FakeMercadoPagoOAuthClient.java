package com.easybeach.support;

import com.easybeach.payments.MercadoPagoOAuthClient;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * No hay credenciales reales de una app de Mercado Pago para golpear en
 * tests - es el único límite externo mockeado en toda la suite (todo lo
 * demás corre contra MySQL real). Simula un intercambio de código exitoso
 * con datos determinísticos.
 */
@TestConfiguration
public class FakeMercadoPagoOAuthClient {

    /**
     * TTL que devuelve el fake, igual al real de Mercado Pago (180 días). No es
     * un detalle cosmético: tiene que ser holgadamente mayor al margen de
     * {@code MpTokenRefreshJob}, o cada token recién renovado volvería a entrar
     * en la ventana de "por vencer" y el job lo refrescaría en cada ciclo.
     */
    private static final long TTL_SEGUNDOS = Duration.ofDays(180).toSeconds();

    /** Handle que los tests inyectan para dirigir el comportamiento del fake. */
    public static class Control {

        private volatile boolean fallarRefresh = false;

        /** Simula que MP rechaza el refresh (cuenta caída o refresh token muerto). */
        public void fallarRefresh() {
            this.fallarRefresh = true;
        }

        public void reset() {
            fallarRefresh = false;
        }
    }

    @Bean
    public Control controlOAuth() {
        return new Control();
    }

    @Bean
    @Primary
    public MercadoPagoOAuthClient mercadoPagoOAuthClient(Control control) {
        AtomicInteger contador = new AtomicInteger();
        return new MercadoPagoOAuthClient() {
            @Override
            public String buildAuthorizationUrl(String state) {
                return "https://auth.mercadopago.com.ar/authorization?state=" + state + "&fake=true";
            }

            @Override
            public TokenResponse exchangeCode(String authorizationCode) {
                int n = contador.incrementAndGet();
                return new TokenResponse("fake-access-token-" + n, "fake-refresh-token-" + n,
                        "fake-mp-user-" + n, "read write", TTL_SEGUNDOS);
            }

            @Override
            public TokenResponse refreshToken(String refreshToken) {
                if (control.fallarRefresh) {
                    throw new IllegalStateException("Fake: Mercado Pago rechazó el refresh");
                }
                int n = contador.incrementAndGet();
                return new TokenResponse("fake-access-token-refreshed-" + n, "fake-refresh-token-refreshed-" + n,
                        "fake-mp-user-" + n, "read write", TTL_SEGUNDOS);
            }
        };
    }
}
