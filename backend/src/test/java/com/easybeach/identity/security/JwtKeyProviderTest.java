package com.easybeach.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regresión etapa 20: un docker-compose.yml real que apunta
 * JWT_PRIVATE_KEY_LOCATION a una ruta absoluta de filesystem sin scheme (p.
 * ej. "/var/easybeach/jwt-keys/private.pem") hacía que DefaultResourceLoader
 * la resolviera como ClassPathResource y fallara con FileNotFoundException -
 * nunca se había probado con una ruta absoluta real hasta un docker compose up.
 */
class JwtKeyProviderTest {

    @Test
    void cargaClavesDesdeRutaAbsolutaDeFilesystemSinSchemeExplicito(@TempDir Path tempDir) throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        Path privateKeyFile = tempDir.resolve("private.pem");
        Path publicKeyFile = tempDir.resolve("public.pem");
        Files.writeString(privateKeyFile, pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        Files.writeString(publicKeyFile, pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

        JwtProperties properties = new JwtProperties();
        // Sin "file:" al frente - exactamente como queda JWT_PRIVATE_KEY_LOCATION
        // en docker-compose.yml.
        properties.setPrivateKeyLocation(privateKeyFile.toAbsolutePath().toString());
        properties.setPublicKeyLocation(publicKeyFile.toAbsolutePath().toString());

        JwtKeyProvider.JwtKeyPair loaded = new JwtKeyProvider().jwtKeyPair(properties);

        RSAPrivateKey expectedPrivate = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey expectedPublic = (RSAPublicKey) keyPair.getPublic();
        assertThat(loaded.privateKey().getModulus()).isEqualTo(expectedPrivate.getModulus());
        assertThat(loaded.publicKey().getModulus()).isEqualTo(expectedPublic.getModulus());
    }

    private static String pem(String marker, byte[] der) {
        String base64 = Base64.getEncoder().encodeToString(der);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < base64.length(); i += 64) {
            body.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return "-----BEGIN " + marker + "-----\n" + body + "-----END " + marker + "-----\n";
    }
}
