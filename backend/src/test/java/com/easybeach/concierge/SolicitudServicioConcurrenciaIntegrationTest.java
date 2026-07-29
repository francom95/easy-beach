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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Etapa 19 §3: mismo escenario que {@code PedidoConcurrenciaIntegrationTest}
 * pero para la cola de servicios al carpero - dos requests simultáneos
 * transicionando la misma solicitud a destinos distintos.
 */
class SolicitudServicioConcurrenciaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("solicitud-concurrencia");
    }

    private String crearSolicitudPendiente() {
        HttpHeaders clienteHeaders = escenario.registrarClienteYObtenerHeaders();
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        TipoServicioResponse tipo = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.POST,
                new HttpEntity<>(new TipoServicioRequest("Hielo", true, 1), ctx.adminHeaders()),
                TipoServicioResponse.class).getBody();
        SolicitudServicioResponse solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"),
                HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(estadia.publicId(), tipo.id(), null), clienteHeaders),
                SolicitudServicioResponse.class).getBody();
        return solicitud.publicId();
    }

    @Test
    void dosTransicionesSimultaneasDeLaMismaSolicitudNoDevuelven500() throws Exception {
        String publicId = crearSolicitudPendiente();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier largada = new CyclicBarrier(2);
        try {
            Callable<Integer> aEnCurso = () -> {
                largada.await(10, TimeUnit.SECONDS);
                var r = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + publicId + "/estado"),
                        HttpMethod.PUT,
                        new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.EN_CURSO),
                                ctx.carperoHeaders()),
                        String.class);
                return r.getStatusCode().value();
            };
            Callable<Integer> aCancelada = () -> {
                largada.await(10, TimeUnit.SECONDS);
                var r = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + publicId + "/estado"),
                        HttpMethod.PUT,
                        new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.CANCELADA),
                                ctx.carperoHeaders()),
                        String.class);
                return r.getStatusCode().value();
            };

            List<Future<Integer>> resultados = pool.invokeAll(List.of(aEnCurso, aCancelada), 30, TimeUnit.SECONDS);
            int s1 = resultados.get(0).get();
            int s2 = resultados.get(1).get();

            assertThat(List.of(s1, s2)).contains(HttpStatus.OK.value());
            assertThat(List.of(s1, s2))
                    .as("ninguna de las dos respuestas debe ser un 500 sin manejar")
                    .allSatisfy(status -> assertThat(status).isIn(HttpStatus.OK.value(), HttpStatus.CONFLICT.value()));
        } finally {
            pool.shutdownNow();
        }
    }
}
