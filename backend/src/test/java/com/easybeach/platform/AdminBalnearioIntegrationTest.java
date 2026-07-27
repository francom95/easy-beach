package com.easybeach.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Etapa 17 (gap real encontrado al construir el panel web): ni el token de
 * staff ni {@code /staff/whoami} exponían nombre/slug/estado del propio
 * balneario para el header/sidebar del panel admin.
 */
class AdminBalnearioIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    @Test
    void adminObtieneSuPropioBalneario() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("mi-balneario");

        BalnearioResponse response = restTemplate.exchange(url("/api/v1/admin/balneario"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), BalnearioResponse.class).getBody();

        assertThat(response.slug()).isEqualTo(ctx.slug());
        assertThat(response.operativo()).isTrue();
    }

    @Test
    void operadorNoAccedeAlEndpointDeAdmin() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("sin-acceso-admin");

        var response = restTemplate.exchange(url("/api/v1/admin/balneario"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    /** El header del panel operativo (CARPERO/OPERADOR, no solo admin) también necesita el nombre del balneario. */
    @Test
    void carperoObtieneElNombreDeSuBalnearioViaStaffBalneario() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("balneario-para-carpero");

        BalnearioResponse response = restTemplate.exchange(url("/api/v1/staff/balneario"), HttpMethod.GET,
                new HttpEntity<>(ctx.carperoHeaders()), BalnearioResponse.class).getBody();

        assertThat(response.slug()).isEqualTo(ctx.slug());
    }
}
