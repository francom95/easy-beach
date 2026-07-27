package com.easybeach.identity.security;

import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Emite y valida el access token (JWT RS256, etapa 05 §1.2) y genera/hashea
 * el refresh token opaco (§1.1: nunca es JWT, se persiste con hash).
 */
@Service
public class JwtService {

    private final JwtKeyProvider.JwtKeyPair keyPair;
    private final JwtProperties properties;
    private final SecureRandom random = new SecureRandom();

    public JwtService(JwtKeyProvider.JwtKeyPair keyPair, JwtProperties properties) {
        this.keyPair = keyPair;
        this.properties = properties;
    }

    public record AccessToken(String value, Instant expiresAt) {
    }

    public AccessToken generateAccessToken(String usuarioPublicId, Long usuarioId, TipoUsuario tipo,
                                            RolCodigo rol, Long balnearioId) {
        Instant now = Instant.now();
        Duration ttl = switch (tipo) {
            case CLIENTE -> Duration.ofMinutes(properties.getAccessTtlMinutesCliente());
            case STAFF -> Duration.ofMinutes(properties.getAccessTtlMinutesStaff());
            case SUPER_ADMIN -> Duration.ofMinutes(properties.getAccessTtlMinutesSuperAdmin());
        };
        Instant expiresAt = now.plus(ttl);

        var builder = Jwts.builder()
                .subject(usuarioPublicId)
                .issuer(properties.getIssuer())
                // uid: id numérico interno. Ver Javadoc de EasyBeachUserPrincipal.usuarioId().
                .claim("uid", usuarioId)
                .claim("tipo", tipo.name())
                .claim("rol", rol.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString());
        if (balnearioId != null) {
            builder.claim("balneario_id", balnearioId);
        }
        String token = builder.signWith(keyPair.privateKey(), Jwts.SIG.RS256).compact();
        return new AccessToken(token, expiresAt);
    }

    /** @throws JwtException si el token es inválido, está mal firmado o expiró. */
    public Claims parseAndValidate(String token) throws JwtException {
        try {
            return Jwts.parser()
                    .verifyWith(keyPair.publicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw e;
        }
    }

    public Duration refreshTtlFor(TipoUsuario tipo) {
        return switch (tipo) {
            case CLIENTE -> properties.getRefreshTtlCliente();
            case STAFF -> properties.getRefreshTtlStaff();
            case SUPER_ADMIN -> properties.getRefreshTtlSuperAdmin();
        };
    }

    /** Refresh token opaco: 256 bits de aleatoriedad, codificados base64url. */
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
