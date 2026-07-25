package com.easybeach.payments;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES-256-GCM para tokens OAuth de Mercado Pago en reposo (etapa 05 §4.2:
 * "cifrados en reposo... clave maestra fuera de la DB"). Sin
 * {@code easybeach.security.token-encryption-key} configurada (local/dev),
 * genera una clave efímera al arrancar - mismo caveat que las claves JWT:
 * reiniciar invalida lo cifrado antes. En prod es obligatoria.
 */
@Service
public class TokenEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryptionService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public TokenEncryptionService(@Value("${easybeach.security.token-encryption-key:}") String base64Key)
            throws NoSuchAlgorithmException {
        if (base64Key.isBlank()) {
            log.warn("easybeach.security.token-encryption-key no configurada: generando clave AES-256 efímera "
                    + "(los tokens cifrados con ella no se podrán leer tras reiniciar). No usar en producción.");
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(256);
            this.key = generator.generateKey();
        } else {
            byte[] decoded = Base64.getDecoder().decode(base64Key);
            this.key = new SecretKeySpec(decoded, "AES");
        }
    }

    /** IV aleatorio de 12 bytes prefijado al ciphertext (formato: iv || ciphertext+tag). */
    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Error cifrando token", e);
        }
    }

    public String decrypt(byte[] ivAndCiphertext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(ivAndCiphertext, 0, iv, 0, IV_BYTES);
            byte[] ciphertext = new byte[ivAndCiphertext.length - IV_BYTES];
            System.arraycopy(ivAndCiphertext, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext));
        } catch (Exception e) {
            throw new IllegalStateException("Error descifrando token", e);
        }
    }
}
