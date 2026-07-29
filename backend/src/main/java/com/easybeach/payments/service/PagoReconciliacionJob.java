package com.easybeach.payments.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconciliación de pagos pendientes (ADR-004). Mercado Pago reintenta el
 * webhook, pero puede rendirse; y el webhook también puede llegar y fallar. Sin
 * este job, ese pago queda PENDIENTE para siempre: el cliente pagó, el pedido
 * nunca entra a la cola de la cocina y nadie se entera hasta que el cliente
 * reclama.
 *
 * <p>Cada pago se concilia en su propia transacción ({@link
 * PagoService#conciliarPago}) porque cada uno habla con MP con el token de SU
 * balneario: que la cuenta de un balneario esté caída no puede impedir que se
 * concilien los pagos de los demás.
 *
 * <p>Corre cada 5 minutos sobre pagos de más de {@value #MARGEN_MINUTOS}
 * minutos: margen suficiente para no competir con el webhook normal (que llega
 * en segundos) ni con el resultado sincrónico de MP.
 */
@Component
public class PagoReconciliacionJob {

    private static final Logger log = LoggerFactory.getLogger(PagoReconciliacionJob.class);

    /** Edad mínima de un pago pendiente para que valga la pena reconsultarlo. */
    public static final int MARGEN_MINUTOS = 10;

    private final PagoService pagoService;

    public PagoReconciliacionJob(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void conciliarPendientes() {
        Instant limite = Instant.now().minus(Duration.ofMinutes(MARGEN_MINUTOS));
        List<Long> pendientes = pagoService.idsDePagosPendientesDesde(limite);
        if (pendientes.isEmpty()) {
            return;
        }

        int actualizados = 0;
        int fallados = 0;
        for (Long pagoId : pendientes) {
            try {
                if (pagoService.conciliarPago(pagoId) == PagoService.ResultadoConciliacion.ACTUALIZADO) {
                    actualizados++;
                }
            } catch (RuntimeException e) {
                // No cortar el lote: el proximo ciclo lo reintenta. Se loguea a
                // ERROR para que la alerta de tasa de errores lo levante si es
                // sistematico (ops/alerts.yml).
                fallados++;
                log.error("No se pudo conciliar el pago {} contra Mercado Pago", pagoId, e);
            }
        }

        log.info("Reconciliación de pagos: {} pendientes revisados, {} actualizados, {} con error",
                pendientes.size(), actualizados, fallados);
    }
}
