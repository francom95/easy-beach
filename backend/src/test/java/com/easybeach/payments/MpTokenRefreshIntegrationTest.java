package com.easybeach.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.repository.BalnearioMpCredencialRepository;
import com.easybeach.payments.service.BalnearioMpCredencialService;
import com.easybeach.payments.service.MpTokenRefreshJob;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import com.easybeach.support.FakeMercadoPagoOAuthClient;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Job de refresh de tokens OAuth de MP (ADR-004). Cubre el segundo agujero que
 * encontró la revisión de cierre: {@code refreshToken()} estaba implementado y
 * nadie lo llamaba, y {@code tokenExpiraAt} se escribía sin leerse nunca, así
 * que al vencer el token el balneario dejaba de cobrar en silencio.
 */
class MpTokenRefreshIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private BalnearioMpCredencialRepository credencialRepository;
    @Autowired
    private BalnearioMpCredencialService credencialService;
    @Autowired
    private MpTokenRefreshJob job;
    @Autowired
    private FakeMercadoPagoOAuthClient.Control controlOAuth;

    private EscenarioBalneario.Contexto ctx;

    @BeforeEach
    void seed() {
        controlOAuth.reset();
        ctx = escenario.crearBalnearioOperativoConStaff("refresh-mp");
    }

    /**
     * Fija el vencimiento del token para simular "está por vencer" / "ya
     * venció". Trunca a milisegundos porque la columna es {@code datetime(3)}:
     * sin esto, el valor en memoria (nanos) no coincide con el que vuelve de
     * MySQL y las comparaciones fallan por redondeo.
     */
    private BalnearioMpCredencial venceEn(Duration desdeAhora) {
        BalnearioMpCredencial credencial = credencialRepository.findByBalnearioId(ctx.balnearioId()).orElseThrow();
        credencial.setTokenExpiraAt(Instant.now().plus(desdeAhora).truncatedTo(ChronoUnit.MILLIS));
        return credencialRepository.save(credencial);
    }

    @Test
    void unTokenPorVencerSeRenuevaYCorreLaFechaDeVencimiento() {
        BalnearioMpCredencial antes = venceEn(Duration.ofDays(2));
        byte[] accessTokenViejo = antes.getAccessTokenCifrado();
        Instant vencimientoViejo = antes.getTokenExpiraAt();

        job.refrescarPorVencer();

        // Se asserta sobre ESTA credencial y no sobre el contador del fake: la
        // base es compartida por toda la suite, asi que el job tambien levanta
        // credenciales que dejaron otros tests.
        BalnearioMpCredencial despues = credencialRepository.findByBalnearioId(ctx.balnearioId()).orElseThrow();
        assertThat(despues.getAccessTokenCifrado()).isNotEqualTo(accessTokenViejo);
        assertThat(despues.getTokenExpiraAt()).isAfter(vencimientoViejo);
        assertThat(despues.getEstado()).isEqualTo(EstadoCredencialMp.VINCULADA);
    }

    @Test
    void unTokenLejosDelVencimientoNoSeToca() {
        BalnearioMpCredencial antes = venceEn(Duration.ofDays(MpTokenRefreshJob.MARGEN_DIAS + 30));
        byte[] accessTokenViejo = antes.getAccessTokenCifrado();
        Instant vencimientoViejo = antes.getTokenExpiraAt();

        job.refrescarPorVencer();

        BalnearioMpCredencial despues = credencialRepository.findByBalnearioId(ctx.balnearioId()).orElseThrow();
        assertThat(despues.getAccessTokenCifrado()).isEqualTo(accessTokenViejo);
        assertThat(despues.getTokenExpiraAt()).isEqualTo(vencimientoViejo);
    }

    /**
     * MP caído pero con token todavía vigente: no se toca el estado, se
     * reintenta en el próximo ciclo. Lo contrario dejaría a un balneario sin
     * cobrar por una caída pasajera de MP.
     */
    @Test
    void siElRefreshFallaYElTokenSigueVigenteLaCredencialQuedaUsable() {
        venceEn(Duration.ofDays(2));
        controlOAuth.fallarRefresh();

        job.refrescarPorVencer();

        BalnearioMpCredencial despues = credencialRepository.findByBalnearioId(ctx.balnearioId()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoCredencialMp.VINCULADA);
        assertThat(credencialService.puedeRecibirPagos(ctx.balnearioId())).isTrue();
    }

    /**
     * Token ya vencido y MP sigue rechazando: la credencial es papel mojado.
     * Se marca EXPIRADA para que el panel lo muestre y para cortar el paso a
     * pedidos nuevos, en vez de dejarlos fallar recién en el cobro.
     */
    @Test
    void siElRefreshFallaYElTokenYaVencioLaCredencialQuedaExpirada() {
        venceEn(Duration.ofDays(-1));
        controlOAuth.fallarRefresh();

        job.refrescarPorVencer();

        BalnearioMpCredencial despues = credencialRepository.findByBalnearioId(ctx.balnearioId()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoCredencialMp.EXPIRADA);
        assertThat(credencialService.puedeRecibirPagos(ctx.balnearioId())).isFalse();
    }
}
