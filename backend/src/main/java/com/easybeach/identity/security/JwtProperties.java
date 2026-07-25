package com.easybeach.identity.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "easybeach.jwt")
public class JwtProperties {

    /** Ubicación (classpath:/file:) de la clave privada RSA en PEM. Vacío en local/dev -> se genera un par efímero. */
    private String privateKeyLocation = "";

    /** Ubicación de la clave pública RSA en PEM. */
    private String publicKeyLocation = "";

    private String issuer = "easybeach-api";

    private int accessTtlMinutesCliente = 60;
    private int accessTtlMinutesStaff = 30;
    private int accessTtlMinutesSuperAdmin = 15;

    private Duration refreshTtlCliente = Duration.ofDays(60);
    private Duration refreshTtlStaff = Duration.ofHours(12);
    private Duration refreshTtlSuperAdmin = Duration.ofHours(8);

    public String getPrivateKeyLocation() {
        return privateKeyLocation;
    }

    public void setPrivateKeyLocation(String privateKeyLocation) {
        this.privateKeyLocation = privateKeyLocation;
    }

    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public int getAccessTtlMinutesCliente() {
        return accessTtlMinutesCliente;
    }

    public void setAccessTtlMinutesCliente(int accessTtlMinutesCliente) {
        this.accessTtlMinutesCliente = accessTtlMinutesCliente;
    }

    public int getAccessTtlMinutesStaff() {
        return accessTtlMinutesStaff;
    }

    public void setAccessTtlMinutesStaff(int accessTtlMinutesStaff) {
        this.accessTtlMinutesStaff = accessTtlMinutesStaff;
    }

    public int getAccessTtlMinutesSuperAdmin() {
        return accessTtlMinutesSuperAdmin;
    }

    public void setAccessTtlMinutesSuperAdmin(int accessTtlMinutesSuperAdmin) {
        this.accessTtlMinutesSuperAdmin = accessTtlMinutesSuperAdmin;
    }

    public Duration getRefreshTtlCliente() {
        return refreshTtlCliente;
    }

    public void setRefreshTtlCliente(Duration refreshTtlCliente) {
        this.refreshTtlCliente = refreshTtlCliente;
    }

    public Duration getRefreshTtlStaff() {
        return refreshTtlStaff;
    }

    public void setRefreshTtlStaff(Duration refreshTtlStaff) {
        this.refreshTtlStaff = refreshTtlStaff;
    }

    public Duration getRefreshTtlSuperAdmin() {
        return refreshTtlSuperAdmin;
    }

    public void setRefreshTtlSuperAdmin(Duration refreshTtlSuperAdmin) {
        this.refreshTtlSuperAdmin = refreshTtlSuperAdmin;
    }
}
