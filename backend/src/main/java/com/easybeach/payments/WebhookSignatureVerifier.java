package com.easybeach.payments;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifica el header {@code x-signature} de Mercado Pago (etapa 05 §4.1,
 * amenaza #4: webhook falsificado que "aprueba" un pago).
 *
 * <p>MP firma con HMAC-SHA256 sobre el manifest
 * {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;} usando el secret de
 * la aplicación. El header viene como {@code ts=<ts>,v1=<hash>}.
 *
 * <p>La firma es la primera barrera, no la única: aunque valide, ADR-004
 * exige <b>reconsultar el pago a MP</b> antes de mover el pedido.
 */
@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    private final String secret;

    public WebhookSignatureVerifier(@Value("${easybeach.mercadopago.webhook-secret:}") String secret) {
        this.secret = secret;
        if (secret.isBlank()) {
            log.warn("easybeach.mercadopago.webhook-secret sin configurar: la verificación de firma del "
                    + "webhook queda DESHABILITADA. Aceptable en local/test, nunca en producción.");
        }
    }

    /**
     * @return {@code true} si la firma es válida, o si no hay secret
     *         configurado (local/dev). En prod el secret es obligatorio.
     */
    public boolean esValida(String xSignature, String xRequestId, String dataId) {
        if (secret.isBlank()) {
            return true;
        }
        if (xSignature == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("ts".equals(kv[0].trim())) {
                ts = kv[1].trim();
            } else if ("v1".equals(kv[0].trim())) {
                v1 = kv[1].trim();
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        String manifest = "id:" + dataId + ";request-id:" + (xRequestId == null ? "" : xRequestId) + ";ts:" + ts + ";";
        String esperado = hmacSha256(manifest);
        // Comparación en tiempo constante: no filtrar información por timing.
        return MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el HMAC del webhook", e);
        }
    }
}
