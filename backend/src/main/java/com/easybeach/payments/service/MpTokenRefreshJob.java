package com.easybeach.payments.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Refresh anticipado de los access token de Mercado Pago (ADR-004). El token
 * OAuth de cada balneario vence; sin este job, el vencimiento se descubre
 * cuando un cliente intenta pagar y el cobro falla — es decir, perdiendo ventas
 * un sábado de enero.
 *
 * <p>Se renueva con {@value #MARGEN_DIAS} días de anticipación: da muchos
 * ciclos de reintento antes del vencimiento real, así una caída puntual de MP
 * no se convierte en una credencial muerta.
 *
 * <p>Corre cada 6 horas. Es una query indexada por estado+fecha que la enorme
 * mayoría de las veces no devuelve nada.
 */
@Component
public class MpTokenRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(MpTokenRefreshJob.class);

    /** Cuánta anticipación al vencimiento dispara la renovación. */
    public static final int MARGEN_DIAS = 7;

    private final BalnearioMpCredencialService credencialService;

    public MpTokenRefreshJob(BalnearioMpCredencialService credencialService) {
        this.credencialService = credencialService;
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void refrescarPorVencer() {
        Instant limite = Instant.now().plus(Duration.ofDays(MARGEN_DIAS));
        List<Long> porVencer = credencialService.idsDeCredencialesPorVencer(limite);
        if (porVencer.isEmpty()) {
            return;
        }

        int renovadas = 0;
        int expiradas = 0;
        int transitorias = 0;
        for (Long credencialId : porVencer) {
            switch (credencialService.refrescarCredencial(credencialId)) {
                case RENOVADA -> renovadas++;
                case EXPIRADA -> expiradas++;
                case FALLO_TRANSITORIO -> transitorias++;
                case OMITIDA -> { }
            }
        }

        if (expiradas > 0) {
            // Estos balnearios dejaron de poder cobrar: hay que avisarles para
            // que vuelvan a vincular la cuenta desde el panel de cobros.
            log.error("{} credencial(es) de Mercado Pago quedaron EXPIRADAS: esos balnearios "
                    + "no pueden cobrar hasta re-vincular la cuenta", expiradas);
        }
        log.info("Refresh de tokens MP: {} por vencer, {} renovadas, {} expiradas, {} con falla transitoria",
                porVencer.size(), renovadas, expiradas, transitorias);
    }
}
