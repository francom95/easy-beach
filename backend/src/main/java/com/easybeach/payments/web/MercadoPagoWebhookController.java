package com.easybeach.payments.web;

import com.easybeach.payments.WebhookSignatureVerifier;
import com.easybeach.payments.service.PagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receptor del webhook de Mercado Pago (ADR-004 / etapa 05 §4.1). Público:
 * MP no tiene JWT. La autenticidad la da la firma {@code x-signature}, y aun
 * así el body no se cree - {@code PagoService} reconsulta el pago a MP.
 *
 * <p>Responde {@code 200} también ante duplicados o pagos desconocidos: MP
 * reintenta ante cualquier error, y un duplicado ya fue manejado
 * correctamente (idempotencia). Solo la firma inválida devuelve {@code 401}.
 */
@RestController
@RequestMapping("/api/v1/mercadopago/webhook")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final PagoService pagoService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(PagoService pagoService, WebhookSignatureVerifier signatureVerifier,
                                         ObjectMapper objectMapper) {
        this.pagoService = pagoService;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Void> recibir(@RequestBody String payload,
                                         @RequestHeader(value = "x-signature", required = false) String xSignature,
                                         @RequestHeader(value = "x-request-id", required = false) String xRequestId) {
        String dataId;
        String tipo;
        try {
            JsonNode root = objectMapper.readTree(payload);
            dataId = root.path("data").path("id").asText(null);
            tipo = root.path("type").asText("payment");
        } catch (Exception e) {
            // Payload ilegible: no reintentar, no hay nada que procesar.
            log.warn("Webhook de MP con payload inválido");
            return ResponseEntity.badRequest().build();
        }

        if (dataId == null || dataId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!signatureVerifier.esValida(xSignature, xRequestId, dataId)) {
            log.warn("Webhook de MP con firma inválida para data.id={}", dataId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        pagoService.procesarWebhook(dataId, tipo, payload);
        return ResponseEntity.ok().build();
    }
}
