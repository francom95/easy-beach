package com.easybeach.stay;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.stay.domain.EstadoUbicacion;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.CambiarEstadoUbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Etapa 16 (gap real encontrado al construir la app mobile): sin este
 * endpoint, S05 "Elegir ubicación" no tiene forma de saber qué ubicaciones
 * existen. Público, sin autenticación, resuelto por slug (mismo patrón que
 * el menú de la etapa 11).
 */
class PublicUbicacionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private Long ubicacionActivaId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("ubicaciones-publicas");
        UbicacionResponse activa = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        ubicacionActivaId = activa.id();

        UbicacionResponse inactiva = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.SOMBRILLA, "Sombrilla 9"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/admin/ubicaciones/" + inactiva.id() + "/estado"), HttpMethod.PUT,
                new HttpEntity<>(new CambiarEstadoUbicacionRequest(EstadoUbicacion.INACTIVA), ctx.adminHeaders()),
                UbicacionResponse.class);
    }

    @Test
    void listaSoloUbicacionesActivasSinAutenticarse() {
        UbicacionResponse[] ubicaciones = restTemplate.getForObject(
                url("/api/v1/balnearios/" + ctx.slug() + "/ubicaciones"), UbicacionResponse[].class);

        assertThat(ubicaciones).extracting(UbicacionResponse::id).containsExactly(ubicacionActivaId);
        assertThat(ubicaciones[0].estado()).isEqualTo(EstadoUbicacion.ACTIVA.name());
    }

    @Test
    void noMezclaUbicacionesDeOtroBalneario() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("ubicaciones-ajenas");
        restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.MESA, "Mesa de otro"), otro.adminHeaders()),
                UbicacionResponse.class);

        UbicacionResponse[] ubicacionesDelPrimero = restTemplate.getForObject(
                url("/api/v1/balnearios/" + ctx.slug() + "/ubicaciones"), UbicacionResponse[].class);

        assertThat(ubicacionesDelPrimero).extracting(UbicacionResponse::identificador)
                .containsExactly("Carpa 1");
    }

    @Test
    void slugInexistenteDevuelve404() {
        var response = restTemplate.getForEntity(url("/api/v1/balnearios/no-existe-nada/ubicaciones"),
                ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
