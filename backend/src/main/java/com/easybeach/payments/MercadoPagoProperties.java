package com.easybeach.payments;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales de la aplicación EasyBeach en Mercado Pago (ADR-004: una sola app, marketplace/split). */
@ConfigurationProperties(prefix = "easybeach.mercadopago")
public class MercadoPagoProperties {

    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "";
    private String authorizationBaseUrl = "https://auth.mercadopago.com.ar/authorization";
    private String tokenUrl = "https://api.mercadopago.com/oauth/token";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getAuthorizationBaseUrl() {
        return authorizationBaseUrl;
    }

    public void setAuthorizationBaseUrl(String authorizationBaseUrl) {
        this.authorizationBaseUrl = authorizationBaseUrl;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
}
