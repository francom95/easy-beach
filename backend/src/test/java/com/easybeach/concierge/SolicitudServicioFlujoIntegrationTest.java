package com.easybeach.concierge;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import com.easybeach.concierge.web.dto.SolicitarServicioRequest;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.concierge.web.dto.TransicionSolicitudRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Etapa 14 criterio de aceptación: "el flujo de solicitud de servicio corre
 * de punta a punta con su tiempo real" - el tiempo real es SSE (fallback por
 * polling siempre disponible, ADR-003), así que se verifica el flujo de
 * negocio de punta a punta vía los mismos GET que el fallback usaría.
 */
class SolicitudServicioFlujoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long tipoServicioId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("servicios");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.SOMBRILLA, "Sombrilla 5"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();

        TipoServicioResponse tipo = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.POST,
                new HttpEntity<>(new TipoServicioRequest("Hielo", true, 1), ctx.adminHeaders()),
                TipoServicioResponse.class).getBody();
        tipoServicioId = tipo.id();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();
    }

    @Test
    void elClientePuedeVerLosTiposDeServicioDeSuPropiaEstadia() {
        var tipos = restTemplate.exchange(
                url("/api/v1/tipos-servicio?estadiaPublicId=" + estadiaPublicId), HttpMethod.GET,
                new HttpEntity<>(clienteHeaders), TipoServicioResponse[].class).getBody();
        assertThat(tipos).extracting(TipoServicioResponse::nombre).containsExactly("Hielo");
    }

    @Test
    void unClienteNoPuedeVerTiposDeServicioDeUnaEstadiaAjena() {
        HttpHeaders otroCliente = escenario.registrarClienteYObtenerHeaders();
        var response = restTemplate.exchange(
                url("/api/v1/tipos-servicio?estadiaPublicId=" + estadiaPublicId), HttpMethod.GET,
                new HttpEntity<>(otroCliente), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void flujoCompletoPendienteEnCursoResuelta() {
        var solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(estadiaPublicId, tipoServicioId, "Con hielo, por favor"),
                        clienteHeaders),
                SolicitudServicioResponse.class);
        assertThat(solicitud.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String publicId = solicitud.getBody().publicId();
        assertThat(solicitud.getBody().estado()).isEqualTo(EstadoSolicitudServicio.PENDIENTE.name());

        // Aparece en la cola del carpero.
        SolicitudServicioResponse[] cola = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/cola"),
                HttpMethod.GET, new HttpEntity<>(ctx.carperoHeaders()), SolicitudServicioResponse[].class).getBody();
        assertThat(cola).extracting(SolicitudServicioResponse::publicId).contains(publicId);

        // El carpero la toma y la resuelve.
        var enCurso = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + publicId + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.EN_CURSO), ctx.carperoHeaders()),
                SolicitudServicioResponse.class);
        assertThat(enCurso.getBody().estado()).isEqualTo(EstadoSolicitudServicio.EN_CURSO.name());

        var resuelta = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + publicId + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.RESUELTA), ctx.carperoHeaders()),
                SolicitudServicioResponse.class);
        assertThat(resuelta.getBody().estado()).isEqualTo(EstadoSolicitudServicio.RESUELTA.name());

        // Ya resuelta: sale de la cola.
        SolicitudServicioResponse[] colaFinal = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/cola"),
                HttpMethod.GET, new HttpEntity<>(ctx.carperoHeaders()), SolicitudServicioResponse[].class).getBody();
        assertThat(colaFinal).extracting(SolicitudServicioResponse::publicId).doesNotContain(publicId);
    }

    @Test
    void elClientePuedeCancelarUnaSolicitudPendiente() {
        var solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(estadiaPublicId, tipoServicioId, null), clienteHeaders),
                SolicitudServicioResponse.class).getBody();

        var cancelada = restTemplate.exchange(url("/api/v1/solicitudes-servicio/" + solicitud.publicId() + "/cancelacion"),
                HttpMethod.POST, new HttpEntity<>(clienteHeaders), SolicitudServicioResponse.class);
        assertThat(cancelada.getBody().estado()).isEqualTo(EstadoSolicitudServicio.CANCELADA.name());
    }

    @Test
    void noSePuedeSolicitarServicioDesdeUnaEstadiaNoActiva() {
        HttpHeaders otroCliente = escenario.registrarClienteYObtenerHeaders();
        UbicacionResponse otraUbicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.MESA, "Mesa 4"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        EstadiaResponse pendiente = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), otraUbicacion.id()), otroCliente),
                EstadiaResponse.class).getBody();

        var response = restTemplate.exchange(url("/api/v1/solicitudes-servicio"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(pendiente.publicId(), tipoServicioId, null), otroCliente),
                ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void carperoDeOtroBalnearioNoVeNiTransicionaSolicitudesAjenas() {
        var solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(estadiaPublicId, tipoServicioId, null), clienteHeaders),
                SolicitudServicioResponse.class).getBody();

        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("servicios-intruso");
        SolicitudServicioResponse[] colaAjena = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/cola"),
                HttpMethod.GET, new HttpEntity<>(otro.carperoHeaders()), SolicitudServicioResponse[].class).getBody();
        assertThat(colaAjena).extracting(SolicitudServicioResponse::publicId).doesNotContain(solicitud.publicId());

        var intento = restTemplate.exchange(
                url("/api/v1/operativo/solicitudes-servicio/" + solicitud.publicId() + "/estado"), HttpMethod.PUT,
                new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.EN_CURSO), otro.carperoHeaders()),
                ProblemDetail.class);
        assertThat(intento.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
