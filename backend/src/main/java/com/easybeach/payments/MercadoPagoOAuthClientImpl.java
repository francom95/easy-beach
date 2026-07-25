package com.easybeach.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Implementación real contra la API de Mercado Pago (ADR-004). Excluida del
 * perfil {@code test} - los tests de integración usan un fake (ver
 * {@code src/test/.../FakeMercadoPagoOAuthClient}) porque no hay
 * credenciales reales de una app de Mercado Pago para golpear en CI.
 */
@Component
@Profile("!test")
public class MercadoPagoOAuthClientImpl implements MercadoPagoOAuthClient {

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoOAuthClientImpl(MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        String encodedRedirect = URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8);
        return properties.getAuthorizationBaseUrl()
                + "?client_id=" + properties.getClientId()
                + "&response_type=code"
                + "&platform_id=mp"
                + "&state=" + state
                + "&redirect_uri=" + encodedRedirect;
    }

    @Override
    public TokenResponse exchangeCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("redirect_uri", properties.getRedirectUri());
        return callTokenEndpoint(form);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        return callTokenEndpoint(form);
    }

    private TokenResponse callTokenEndpoint(MultiValueMap<String, String> form) {
        MpOAuthResponse response = restClient.post()
                .uri(properties.getTokenUrl())
                .body(form)
                .retrieve()
                .body(MpOAuthResponse.class);
        if (response == null) {
            throw new IllegalStateException("Mercado Pago devolvió una respuesta vacía en /oauth/token");
        }
        return new TokenResponse(response.accessToken, response.refreshToken, response.userId,
                response.scope, response.expiresIn);
    }

    /** Nombres tal cual los devuelve la API de MP (snake_case) - no confiar en el naming strategy global de Jackson. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MpOAuthResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("user_id") String userId,
            @JsonProperty("scope") String scope,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }
}
