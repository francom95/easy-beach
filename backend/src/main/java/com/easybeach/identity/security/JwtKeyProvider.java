package com.easybeach.identity.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Provee el par RSA usado para firmar/validar el access token (RS256, etapa
 * 05 §1.1). Si {@code easybeach.jwt.private-key-location} /
 * {@code public-key-location} no están configuradas (perfil local/dev), se
 * genera un par efímero al arrancar - documentado: reiniciar invalida los
 * tokens emitidos. En prod, las variables son obligatorias (ver
 * application-prod.yml, que no las provee con default) y las claves se
 * cargan desde archivos PEM fuera del repo.
 */
@Configuration
public class JwtKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtKeyProvider.class);

    @Bean
    public JwtKeyPair jwtKeyPair(JwtProperties properties) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (properties.getPrivateKeyLocation().isBlank() || properties.getPublicKeyLocation().isBlank()) {
            log.warn("easybeach.jwt.{private,public}-key-location no configuradas: generando par RSA efímero "
                    + "(los tokens emitidos se invalidan al reiniciar). No usar en producción.");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            return new JwtKeyPair((java.security.interfaces.RSAPrivateKey) pair.getPrivate(),
                    (java.security.interfaces.RSAPublicKey) pair.getPublic());
        }
        ResourceLoader loader = new DefaultResourceLoader();
        PrivateKey privateKey = readPrivateKey(loader.getResource(properties.getPrivateKeyLocation()));
        PublicKey publicKey = readPublicKey(loader.getResource(properties.getPublicKeyLocation()));
        return new JwtKeyPair((java.security.interfaces.RSAPrivateKey) privateKey,
                (java.security.interfaces.RSAPublicKey) publicKey);
    }

    private PrivateKey readPrivateKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = readPem(resource, "PRIVATE KEY");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey readPublicKey(Resource resource) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        String pem = readPem(resource, "PUBLIC KEY");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
    }

    private String readPem(Resource resource, String marker) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            String content = new String(in.readAllBytes());
            return content
                    .replace("-----BEGIN " + marker + "-----", "")
                    .replace("-----END " + marker + "-----", "")
                    .replaceAll("\\s", "");
        }
    }

    public record JwtKeyPair(java.security.interfaces.RSAPrivateKey privateKey,
                              java.security.interfaces.RSAPublicKey publicKey) {
    }
}
