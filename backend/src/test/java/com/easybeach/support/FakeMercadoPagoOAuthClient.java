package com.easybeach.support;

import com.easybeach.payments.MercadoPagoOAuthClient;
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

    @Bean
    @Primary
    public MercadoPagoOAuthClient mercadoPagoOAuthClient() {
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
                        "fake-mp-user-" + n, "read write", 21600L);
            }

            @Override
            public TokenResponse refreshToken(String refreshToken) {
                int n = contador.incrementAndGet();
                return new TokenResponse("fake-access-token-refreshed-" + n, "fake-refresh-token-refreshed-" + n,
                        "fake-mp-user-" + n, "read write", 21600L);
            }
        };
    }
}
