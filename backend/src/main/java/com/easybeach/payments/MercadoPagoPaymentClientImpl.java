package com.easybeach.payments;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Implementación real contra la API de pagos de Mercado Pago (ADR-004).
 * Excluida del perfil {@code test} - ver {@code FakeMercadoPagoPaymentClient}.
 *
 * <p><b>{@code application_fee} es una constante 0 del servidor</b> (etapa 05
 * §4.3), jamás un parámetro: EasyBeach no retiene nada, el balneario cobra
 * todo en su propia cuenta.
 */
@Component
@Profile("!test")
public class MercadoPagoPaymentClientImpl implements MercadoPagoPaymentClient {

    private static final BigDecimal APPLICATION_FEE = BigDecimal.ZERO;
    private static final String PAYMENTS_URL = "https://api.mercadopago.com/v1/payments";

    private final RestClient restClient = RestClient.create();

    @Override
    public ResultadoPago crearPago(String accessTokenBalneario, String idempotencyKey,
                                    BigDecimal monto, String descripcion, String cardToken) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", monto);
        body.put("token", cardToken);
        body.put("description", descripcion);
        body.put("installments", 1);
        body.put("application_fee", APPLICATION_FEE);

        MpPaymentResponse response = restClient.post()
                .uri(PAYMENTS_URL)
                .header("Authorization", "Bearer " + accessTokenBalneario)
                // Idempotencia también del lado de MP: un reintento de red no
                // genera un segundo cobro allá.
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MpPaymentResponse.class);
        return toResultado(response);
    }

    @Override
    public ResultadoPago consultarPago(String accessTokenBalneario, String mpPaymentId) {
        MpPaymentResponse response = restClient.get()
                .uri(PAYMENTS_URL + "/" + mpPaymentId)
                .header("Authorization", "Bearer " + accessTokenBalneario)
                .retrieve()
                .body(MpPaymentResponse.class);
        return toResultado(response);
    }

    @Override
    public ResultadoPago reembolsar(String accessTokenBalneario, String mpPaymentId) {
        restClient.post()
                .uri(PAYMENTS_URL + "/" + mpPaymentId + "/refunds")
                .header("Authorization", "Bearer " + accessTokenBalneario)
                .retrieve()
                .toBodilessEntity();
        return consultarPago(accessTokenBalneario, mpPaymentId);
    }

    private ResultadoPago toResultado(MpPaymentResponse response) {
        if (response == null) {
            throw new IllegalStateException("Mercado Pago devolvió una respuesta vacía");
        }
        return new ResultadoPago(response.id, mapEstado(response.status), response.statusDetail,
                response.paymentMethodId, response.transactionAmount);
    }

    private EstadoMp mapEstado(String status) {
        if (status == null) {
            return EstadoMp.PENDING;
        }
        return switch (status) {
            case "approved" -> EstadoMp.APPROVED;
            case "rejected", "cancelled" -> EstadoMp.REJECTED;
            case "refunded", "charged_back" -> EstadoMp.REFUNDED;
            default -> EstadoMp.PENDING; // pending, in_process, authorized...
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MpPaymentResponse(
            @JsonProperty("id") String id,
            @JsonProperty("status") String status,
            @JsonProperty("status_detail") String statusDetail,
            @JsonProperty("payment_method_id") String paymentMethodId,
            @JsonProperty("transaction_amount") BigDecimal transactionAmount
    ) {
    }
}
