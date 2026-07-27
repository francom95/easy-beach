package com.easybeach.payments;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Etapa 05 §7 amenaza #4: un webhook falsificado no debe poder "aprobar" un
 * pago. Unitario, sin contexto de Spring.
 */
class WebhookSignatureVerifierTest {

    private static final String SECRET = "un-secret-de-prueba-suficientemente-largo";

    private String firmaValida(String dataId, String requestId, String ts) {
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void firmaCorrectaEsAceptada() {
        var verifier = new WebhookSignatureVerifier(SECRET);
        String v1 = firmaValida("12345", "req-1", "1700000000");
        assertThat(verifier.esValida("ts=1700000000,v1=" + v1, "req-1", "12345")).isTrue();
    }

    @Test
    void firmaFalsificadaEsRechazada() {
        var verifier = new WebhookSignatureVerifier(SECRET);
        assertThat(verifier.esValida("ts=1700000000,v1=deadbeef", "req-1", "12345")).isFalse();
    }

    @Test
    void firmaDeOtroPagoEsRechazada() {
        var verifier = new WebhookSignatureVerifier(SECRET);
        // Firma válida pero calculada para OTRO data.id: no sirve para este.
        String v1DeOtroPago = firmaValida("99999", "req-1", "1700000000");
        assertThat(verifier.esValida("ts=1700000000,v1=" + v1DeOtroPago, "req-1", "12345")).isFalse();
    }

    @Test
    void headerMalFormadoEsRechazado() {
        var verifier = new WebhookSignatureVerifier(SECRET);
        assertThat(verifier.esValida("basura", "req-1", "12345")).isFalse();
        assertThat(verifier.esValida(null, "req-1", "12345")).isFalse();
    }

    @Test
    void sinSecretConfiguradoNoBloquea() {
        // Local/dev: sin secret, la verificación se desactiva a propósito
        // (documentado y advertido por log). En prod el secret es obligatorio.
        var verifier = new WebhookSignatureVerifier("");
        assertThat(verifier.esValida(null, null, "12345")).isTrue();
    }
}
