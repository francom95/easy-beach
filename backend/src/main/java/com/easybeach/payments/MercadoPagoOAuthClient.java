package com.easybeach.payments;

/**
 * Abstracción del flujo OAuth de Mercado Pago (ADR-004). La implementación
 * real llama a la API de MP; en tests se reemplaza por un fake (no hay
 * credenciales reales de un balneario de prueba para golpear MP de verdad -
 * es el único límite externo que se mockea en toda la suite, todo lo demás
 * corre contra MySQL real).
 */
public interface MercadoPagoOAuthClient {

    String buildAuthorizationUrl(String state);

    TokenResponse exchangeCode(String authorizationCode);

    TokenResponse refreshToken(String refreshToken);

    record TokenResponse(String accessToken, String refreshToken, String mpUserId, String scope, long expiresInSeconds) {
    }
}
