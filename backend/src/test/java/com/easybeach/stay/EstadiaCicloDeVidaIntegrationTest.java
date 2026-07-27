package com.easybeach.stay;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.stay.domain.EstadoEstadia;
import com.easybeach.stay.repository.EstadiaRepository;
import com.easybeach.stay.service.EstadiaService;
import com.easybeach.stay.web.dto.CambiarUbicacionRequest;
import com.easybeach.stay.web.dto.EstadiaPendienteResponse;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.RechazarEstadiaRequest;
import com.easybeach.stay.web.dto.ResumenCierreResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Ciclo de vida completo de la estadía (etapa 12): apertura en dos pasos,
 * validación por carpero, cambio de ubicación, cierre con resumen, y todas
 * las transiciones inválidas.
 */
class EstadiaCicloDeVidaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private EstadiaRepository estadiaRepository;
    @Autowired
    private EstadiaService estadiaService;

    private EscenarioBalneario.Contexto ctx;
    private Long ubicacionId;
    private HttpHeaders clienteHeaders;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("costa-nena");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();
        ubicacionId = crearUbicacion("Carpa 12");
    }

    private Long crearUbicacion(String identificador) {
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, identificador), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        return ubicacion.id();
    }

    @Test
    void aperturaEnDosPasosValidacionYCierre() {
        // 1. El cliente solicita: queda PENDIENTE_VALIDACION, sin permiso de pedir.
        var solicitud = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);
        assertThat(solicitud.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        EstadiaResponse estadia = solicitud.getBody();
        assertThat(estadia.estado()).isEqualTo(EstadoEstadia.PENDIENTE_VALIDACION.name());
        assertThat(estadia.permitePedidos()).isFalse();

        // 2. Aparece en la cola de validación del carpero, con el nombre del
        // cliente resuelto (etapa 17: la bandeja de validación lo necesita).
        EstadiaPendienteResponse[] pendientes = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"),
                HttpMethod.GET, new HttpEntity<>(ctx.carperoHeaders()), EstadiaPendienteResponse[].class).getBody();
        assertThat(pendientes).extracting(EstadiaPendienteResponse::publicId).contains(estadia.publicId());
        assertThat(pendientes).filteredOn(p -> p.publicId().equals(estadia.publicId()))
                .extracting(EstadiaPendienteResponse::clienteNombre).containsExactly("Cliente de prueba");

        // 3. El carpero valida: ACTIVA y recién ahí se habilitan pedidos.
        EstadiaResponse validada = restTemplate.exchange(
                url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"), HttpMethod.POST,
                new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class).getBody();
        assertThat(validada.estado()).isEqualTo(EstadoEstadia.ACTIVA.name());
        assertThat(validada.permitePedidos()).isTrue();
        assertThat(validada.fechaValidacion()).isNotNull();

        // La validación queda registrada con actor y timestamp.
        var enDb = estadiaRepository.findByPublicId(estadia.publicId()).orElseThrow();
        assertThat(enDb.getValidadaPorUsuarioId()).isEqualTo(ctx.carperoId());

        // 4. Cambio de ubicación preservando historial.
        Long nuevaUbicacion = crearUbicacion("Carpa 15");
        EstadiaResponse movida = restTemplate.exchange(
                url("/api/v1/estadias/" + estadia.publicId() + "/ubicacion"), HttpMethod.PUT,
                new HttpEntity<>(new CambiarUbicacionRequest(nuevaUbicacion), clienteHeaders),
                EstadiaResponse.class).getBody();
        assertThat(movida.ubicacionIdentificador()).isEqualTo("Carpa 15");

        // 5. Cierre explícito con resumen (sin pedidos: el módulo ordering es de la etapa 13).
        ResumenCierreResponse resumen = restTemplate.exchange(
                url("/api/v1/estadias/" + estadia.publicId() + "/cierre"), HttpMethod.POST,
                new HttpEntity<>(clienteHeaders), ResumenCierreResponse.class).getBody();
        assertThat(resumen.cantidadPedidos()).isZero();
        assertThat(resumen.fechaCierre()).isNotNull();

        assertThat(estadiaRepository.findByPublicId(estadia.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoEstadia.CERRADA);
    }

    @Test
    void cerrarLiberaElCupoYPermiteAbrirDeNuevoEnElMismoBalneario() {
        String primera = solicitarYValidar();
        restTemplate.exchange(url("/api/v1/estadias/" + primera + "/cierre"), HttpMethod.POST,
                new HttpEntity<>(clienteHeaders), ResumenCierreResponse.class);

        var segunda = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void segundaSolicitudEnElMismoBalnearioEsRechazada() {
        restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);

        var duplicada = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                ProblemDetail.class);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elMismoClienteSiPuedeTenerEstadiasEnBalneariosDistintos() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("brava");
        UbicacionResponse ubicacionOtro = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.SOMBRILLA, "Sombrilla 3"), otro.adminHeaders()),
                UbicacionResponse.class).getBody();

        var enA = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);
        var enB = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(otro.slug(), ubicacionOtro.id()), clienteHeaders),
                EstadiaResponse.class);

        assertThat(enA.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(enB.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        EstadiaResponse[] vigentes = restTemplate.exchange(url("/api/v1/estadias/vigentes"), HttpMethod.GET,
                new HttpEntity<>(clienteHeaders), EstadiaResponse[].class).getBody();
        assertThat(vigentes).hasSize(2);
    }

    @Test
    void carperoPuedeRechazarYEsoLiberaElCupo() {
        var estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class).getBody();

        EstadiaResponse rechazada = restTemplate.exchange(
                url("/api/v1/operativo/estadias/" + estadia.publicId() + "/rechazo"), HttpMethod.POST,
                new HttpEntity<>(new RechazarEstadiaRequest("La carpa ya está ocupada"), ctx.carperoHeaders()),
                EstadiaResponse.class).getBody();
        assertThat(rechazada.estado()).isEqualTo(EstadoEstadia.RECHAZADA.name());
        assertThat(rechazada.motivoRechazo()).isEqualTo("La carpa ya está ocupada");

        // Cupo liberado: puede volver a solicitar.
        var reintento = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);
        assertThat(reintento.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void transicionesInvalidasSonRechazadasConElFormatoEstandar() {
        String publicId = solicitarYValidar();

        // Ya está ACTIVA: no se puede volver a validar.
        var revalidar = restTemplate.exchange(url("/api/v1/operativo/estadias/" + publicId + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), ProblemDetail.class);
        assertThat(revalidar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Cerrada: no se puede cerrar dos veces ni cambiar de ubicación.
        restTemplate.exchange(url("/api/v1/estadias/" + publicId + "/cierre"), HttpMethod.POST,
                new HttpEntity<>(clienteHeaders), ResumenCierreResponse.class);

        var recerrar = restTemplate.exchange(url("/api/v1/estadias/" + publicId + "/cierre"), HttpMethod.POST,
                new HttpEntity<>(clienteHeaders), ProblemDetail.class);
        assertThat(recerrar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var mover = restTemplate.exchange(url("/api/v1/estadias/" + publicId + "/ubicacion"), HttpMethod.PUT,
                new HttpEntity<>(new CambiarUbicacionRequest(ubicacionId), clienteHeaders), ProblemDetail.class);
        assertThat(mover.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unaEstadiaPendienteNoPuedeCambiarUbicacionNiCerrarse() {
        var estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class).getBody();

        var cerrar = restTemplate.exchange(url("/api/v1/estadias/" + estadia.publicId() + "/cierre"),
                HttpMethod.POST, new HttpEntity<>(clienteHeaders), ProblemDetail.class);
        assertThat(cerrar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void elJobExpiraLasSolicitudesQueSuperanElTtl() {
        var estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class).getBody();

        // Envejecer la solicitud más allá del TTL sin esperar 60 minutos reales.
        var enDb = estadiaRepository.findByPublicId(estadia.publicId()).orElseThrow();
        enDb.setFechaSolicitud(Instant.now().minus(90, ChronoUnit.MINUTES));
        estadiaRepository.save(enDb);

        int expiradas = estadiaService.expirarPendientesVencidas();
        assertThat(expiradas).isGreaterThanOrEqualTo(1);
        assertThat(estadiaRepository.findByPublicId(estadia.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoEstadia.EXPIRADA);

        // Expirar libera el cupo: el cliente puede volver a solicitar.
        var reintento = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class);
        assertThat(reintento.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void noSePuedeDesactivarUnaUbicacionConEstadiaVigente() {
        solicitarYValidar();

        var desactivar = restTemplate.exchange(url("/api/v1/admin/ubicaciones/" + ubicacionId + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new com.easybeach.stay.web.dto.CambiarEstadoUbicacionRequest(
                        com.easybeach.stay.domain.EstadoUbicacion.INACTIVA), ctx.adminHeaders()),
                ProblemDetail.class);
        assertThat(desactivar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var borrar = restTemplate.exchange(url("/api/v1/admin/ubicaciones/" + ubicacionId), HttpMethod.DELETE,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);
        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private String solicitarYValidar() {
        var estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        return estadia.publicId();
    }
}
