package com.easybeach.payments.service;

import com.easybeach.payments.MercadoPagoPaymentClient;
import com.easybeach.payments.TokenEncryptionService;
import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.domain.EstadoPago;
import com.easybeach.payments.domain.MpWebhookNotificacion;
import com.easybeach.payments.domain.PedidoPago;
import com.easybeach.payments.event.PagoResuelto;
import com.easybeach.payments.repository.BalnearioMpCredencialRepository;
import com.easybeach.payments.repository.MpWebhookNotificacionRepository;
import com.easybeach.payments.repository.PedidoPagoRepository;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobro y conciliación con Mercado Pago (ADR-004). Reglas duras:
 * <ul>
 *   <li>El pago se crea SIEMPRE con el token del balneario dueño del pedido
 *       (etapa 05 §4.2: nunca el de otro balneario).</li>
 *   <li>El body del webhook <b>no es fuente de verdad</b>: se reconsulta el
 *       pago a MP antes de mover nada.</li>
 *   <li>Notificación repetida no se re-procesa (UK de idempotencia).</li>
 * </ul>
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PedidoPagoRepository pagoRepository;
    private final MpWebhookNotificacionRepository webhookRepository;
    private final BalnearioMpCredencialRepository credencialRepository;
    private final MercadoPagoPaymentClient paymentClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final ApplicationEventPublisher eventPublisher;

    public PagoService(PedidoPagoRepository pagoRepository,
                        MpWebhookNotificacionRepository webhookRepository,
                        BalnearioMpCredencialRepository credencialRepository,
                        MercadoPagoPaymentClient paymentClient,
                        TokenEncryptionService tokenEncryptionService,
                        ApplicationEventPublisher eventPublisher) {
        this.pagoRepository = pagoRepository;
        this.webhookRepository = webhookRepository;
        this.credencialRepository = credencialRepository;
        this.paymentClient = paymentClient;
        this.tokenEncryptionService = tokenEncryptionService;
        this.eventPublisher = eventPublisher;
    }

    public record ResultadoIniciarPago(Long pagoId, String mpPaymentId, EstadoPago estado) {
    }

    /**
     * Inicia el cobro del pedido. La idempotencia viaja también a MP
     * (header {@code X-Idempotency-Key}) para que un reintento de red no
     * genere un segundo cobro del lado de ellos.
     */
    @Transactional
    public ResultadoIniciarPago iniciarPago(Long pedidoId, Long balnearioId, BigDecimal monto,
                                             String idempotencyKey, String descripcion, String cardToken) {
        String accessToken = accessTokenDe(balnearioId);

        PedidoPago pago = new PedidoPago();
        pago.setPedidoId(pedidoId);
        pago.setBalnearioId(balnearioId);
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago = pagoRepository.save(pago);

        MercadoPagoPaymentClient.ResultadoPago resultado =
                paymentClient.crearPago(accessToken, idempotencyKey, monto, descripcion, cardToken);

        pago.setMpPaymentId(resultado.mpPaymentId());
        pago.setMpStatusDetail(resultado.statusDetail());
        pago.setMetodo(resultado.metodo());
        pago.setEstado(mapEstado(resultado.estado()));
        pago = pagoRepository.save(pago);

        // MP suele responder el resultado final de forma sincrónica; si ya vino
        // resuelto no hace falta esperar el webhook.
        if (pago.getEstado() == EstadoPago.APROBADO || pago.getEstado() == EstadoPago.RECHAZADO) {
            publicarResolucion(pago);
        }
        return new ResultadoIniciarPago(pago.getId(), pago.getMpPaymentId(), pago.getEstado());
    }

    /**
     * Procesa una notificación de webhook. Devuelve {@code false} si era
     * duplicada (ya registrada) y no se hizo nada.
     */
    @Transactional
    public boolean procesarWebhook(String mpPaymentId, String tipo, String payloadCrudo) {
        String hash = sha256(payloadCrudo == null ? "" : payloadCrudo);
        if (webhookRepository.existsByMpPaymentIdAndTipoAndPayloadHash(mpPaymentId, tipo, hash)) {
            log.debug("Webhook duplicado ignorado para payment {}", mpPaymentId);
            return false;
        }

        MpWebhookNotificacion notificacion = new MpWebhookNotificacion();
        notificacion.setMpPaymentId(mpPaymentId);
        notificacion.setTipo(tipo);
        notificacion.setPayloadHash(hash);
        notificacion.setRecibidoAt(Instant.now());

        Optional<PedidoPago> pagoOpt = pagoRepository.findByMpPaymentId(mpPaymentId);
        if (pagoOpt.isEmpty()) {
            // Puede llegar antes de que terminemos de guardar el pago, o ser de
            // otra aplicación. Se registra para auditoría y se ignora.
            notificacion.setProcesado(true);
            notificacion.setResultado("PAGO_DESCONOCIDO");
            webhookRepository.save(notificacion);
            return false;
        }

        PedidoPago pago = pagoOpt.get();
        notificacion.setBalnearioId(pago.getBalnearioId());

        // ADR-004: el payload es solo un aviso. La verdad se consulta a MP.
        MercadoPagoPaymentClient.ResultadoPago real =
                paymentClient.consultarPago(accessTokenDe(pago.getBalnearioId()), mpPaymentId);

        ResultadoConciliacion resultado = aplicarEstadoReal(pago, real);

        notificacion.setProcesado(true);
        notificacion.setResultado(resultado == ResultadoConciliacion.ACTUALIZADO
                ? pago.getEstado().name()
                : resultado.name());
        webhookRepository.save(notificacion);

        return resultado == ResultadoConciliacion.ACTUALIZADO;
    }

    /**
     * Pagos que siguen PENDIENTE pasado el margen: o el webhook nunca llegó
     * (MP reintenta, pero puede rendirse) o llegó y falló. Cross-tenant a
     * propósito - lo corre {@code PagoReconciliacionJob}, no un request, así
     * que no se aplica el filtro de tenant (mismo criterio que el job de
     * expiración de estadías).
     *
     * <p>Devuelve ids y no entidades: cada pago se concilia en su propia
     * transacción, así una llamada fallida a MP no arrastra al resto del lote.
     */
    @Transactional(readOnly = true)
    public List<Long> idsDePagosPendientesDesde(Instant limite) {
        return pagoRepository.findByEstadoAndCreatedAtBefore(EstadoPago.PENDIENTE, limite)
                .stream()
                .map(PedidoPago::getId)
                .toList();
    }

    /**
     * Reconsulta a MP el estado real de un pago pendiente y lo aplica con las
     * mismas invariantes que el webhook (no retrocede un pago resuelto, no
     * confirma si el monto no coincide, publica {@link PagoResuelto} al
     * resolverse). Es lo que hace que un webhook perdido no deje al pedido
     * pagado sin llegar nunca a la cocina.
     */
    @Transactional
    public ResultadoConciliacion conciliarPago(Long pagoId) {
        PedidoPago pago = pagoRepository.findById(pagoId).orElse(null);
        if (pago == null || pago.getEstado() != EstadoPago.PENDIENTE) {
            // Puede haberse resuelto por webhook entre el listado y este punto.
            return ResultadoConciliacion.SIN_CAMBIO;
        }
        if (pago.getMpPaymentId() == null) {
            // Nunca llegó a crearse del lado de MP: no hay nada que reconsultar.
            return ResultadoConciliacion.SIN_CAMBIO;
        }
        MercadoPagoPaymentClient.ResultadoPago real =
                paymentClient.consultarPago(accessTokenDe(pago.getBalnearioId()), pago.getMpPaymentId());
        return aplicarEstadoReal(pago, real);
    }

    /** Cancelación por el local de un pedido ya cobrado (ADR-004). */
    @Transactional
    public void reembolsar(Long pedidoId) {
        pagoRepository.findByPedidoIdAndEstado(pedidoId, EstadoPago.APROBADO).ifPresent(pago -> {
            paymentClient.reembolsar(accessTokenDe(pago.getBalnearioId()), pago.getMpPaymentId());
            pago.setEstado(EstadoPago.REEMBOLSADO);
            pagoRepository.save(pago);
        });
    }

    @Transactional(readOnly = true)
    public boolean tienePagoAprobado(Long pedidoId) {
        return pagoRepository.existsByPedidoIdAndEstado(pedidoId, EstadoPago.APROBADO);
    }

    /**
     * Aplica sobre el pago local el estado que MP reporta como real. Lo
     * comparten el webhook y el job de reconciliación a propósito: son dos
     * puertas de entrada al mismo hecho ("MP dice que este pago está así"), y
     * las invariantes de plata no pueden divergir entre una y otra.
     */
    private ResultadoConciliacion aplicarEstadoReal(PedidoPago pago,
                                                     MercadoPagoPaymentClient.ResultadoPago real) {
        EstadoPago nuevoEstado = mapEstado(real.estado());
        if (pago.getEstado() == nuevoEstado) {
            return ResultadoConciliacion.SIN_CAMBIO;
        }
        // Un pago ya resuelto no vuelve atrás por una notificación fuera de orden.
        if (pago.getEstado() == EstadoPago.APROBADO && nuevoEstado == EstadoPago.PENDIENTE) {
            return ResultadoConciliacion.IGNORADO_FUERA_DE_ORDEN;
        }
        // El monto real debe coincidir con lo que registramos: si MP dice otra
        // cosa, no se confirma el pedido.
        if (nuevoEstado == EstadoPago.APROBADO && real.monto() != null
                && real.monto().compareTo(pago.getMonto()) != 0) {
            log.error("Monto de MP ({}) distinto al del pedido ({}) para payment {}",
                    real.monto(), pago.getMonto(), pago.getMpPaymentId());
            return ResultadoConciliacion.MONTO_INCONSISTENTE;
        }

        pago.setEstado(nuevoEstado);
        pago.setMpStatusDetail(real.statusDetail());
        pagoRepository.save(pago);

        if (nuevoEstado == EstadoPago.APROBADO || nuevoEstado == EstadoPago.RECHAZADO) {
            publicarResolucion(pago);
        }
        return ResultadoConciliacion.ACTUALIZADO;
    }

    /** Qué pasó al contrastar un pago local contra el estado real en MP. */
    public enum ResultadoConciliacion {
        /** MP reporta lo mismo que ya teníamos. */
        SIN_CAMBIO,
        /** Notificación atrasada que pretendía retroceder un pago aprobado. */
        IGNORADO_FUERA_DE_ORDEN,
        /** MP aprobó un monto distinto al del pedido: no se confirma. */
        MONTO_INCONSISTENTE,
        /** El pago cambió de estado y se propagó al pedido. */
        ACTUALIZADO
    }

    private void publicarResolucion(PedidoPago pago) {
        eventPublisher.publishEvent(new PagoResuelto(pago.getPedidoId(), pago.getBalnearioId(),
                pago.getEstado() == EstadoPago.APROBADO, pago.getMpPaymentId(), pago.getMonto(),
                pago.getMpStatusDetail()));
    }

    /** Nunca devuelve el token de otro balneario: se resuelve por el balneario del pago. */
    private String accessTokenDe(Long balnearioId) {
        BalnearioMpCredencial credencial = credencialRepository.findByBalnearioId(balnearioId)
                .filter(c -> c.getEstado() == EstadoCredencialMp.VINCULADA)
                .orElseThrow(() -> new ApiException(ErrorCode.CONFLICTO_DE_ESTADO,
                        "El balneario no tiene una cuenta de Mercado Pago vinculada"));
        return tokenEncryptionService.decrypt(credencial.getAccessTokenCifrado());
    }

    private EstadoPago mapEstado(MercadoPagoPaymentClient.EstadoMp estado) {
        return switch (estado) {
            case APPROVED -> EstadoPago.APROBADO;
            case REJECTED -> EstadoPago.RECHAZADO;
            case REFUNDED -> EstadoPago.REEMBOLSADO;
            case PENDING -> EstadoPago.PENDIENTE;
        };
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo hashear el payload del webhook", e);
        }
    }
}
