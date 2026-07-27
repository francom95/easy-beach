package com.easybeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.concierge.web.dto.SolicitarServicioRequest;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.CambiarUbicacionRequest;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Etapa 19 §2: "IDs ajenos (pedidos, estadías)" desde el lado del cliente.
 * A diferencia del staff (aislado por {@code balnearioId} del JWT), el
 * cliente está aislado por {@code usuarioId} propio - un cliente puede tener
 * estadías en balnearios distintos legítimamente (etapa 12), así que la
 * amenaza real acá es IDOR entre DOS CLIENTES, no "cross-tenant" en sentido
 * estricto. Ningún endpoint de {@code ClientePedidoController},
 * {@code ClienteEstadiaController} ni {@code ClienteSolicitudServicioController}
 * tenía cobertura de esto para pedidos/estadía-directa antes de esta clase.
 */
class CrossTenantClienteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto balneario;
    private HttpHeaders clienteA;
    private HttpHeaders clienteB;

    @BeforeEach
    void seed() {
        balneario = escenario.crearBalnearioOperativoConStaff("cliente-idor");
        clienteA = escenario.registrarClienteYObtenerHeaders();
        clienteB = escenario.registrarClienteYObtenerHeaders();
    }

    // ---------------------------------------------------------------- pedidos

    @Test
    void clienteBNoPuedeVerElPedidoDeClienteA() {
        String publicIdPedidoDeA = crearPedidoDeClienteA();

        var obtener = restTemplate.exchange(url("/api/v1/pedidos/" + publicIdPedidoDeA), HttpMethod.GET,
                new HttpEntity<>(clienteB), ProblemDetail.class);
        assertThat(obtener.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clienteBNoPuedeVerElHistorialDelPedidoDeClienteA() {
        String publicIdPedidoDeA = crearPedidoDeClienteA();

        var historial = restTemplate.exchange(url("/api/v1/pedidos/" + publicIdPedidoDeA + "/historial"),
                HttpMethod.GET, new HttpEntity<>(clienteB), ProblemDetail.class);
        assertThat(historial.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clienteBNoPuedeCancelarElPedidoDeClienteA() {
        String publicIdPedidoDeA = crearPedidoDeClienteA();

        var cancelar = restTemplate.exchange(url("/api/v1/pedidos/" + publicIdPedidoDeA + "/cancelacion"),
                HttpMethod.POST, new HttpEntity<>(clienteB), ProblemDetail.class);
        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // El pedido de A sigue existiendo y accesible para A, sin marcas del intento de B.
        var propio = restTemplate.exchange(url("/api/v1/pedidos/" + publicIdPedidoDeA), HttpMethod.GET,
                new HttpEntity<>(clienteA), PedidoResponse.class);
        assertThat(propio.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---------------------------------------------------------------- estadías

    @Test
    void clienteBNoPuedeCambiarLaUbicacionDeLaEstadiaDeClienteA() {
        EstadiaResponse estadiaDeA = solicitarEstadia(clienteA);
        UbicacionResponse otraUbicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa alternativa"),
                        balneario.adminHeaders()),
                UbicacionResponse.class).getBody();

        var cambiar = restTemplate.exchange(url("/api/v1/estadias/" + estadiaDeA.publicId() + "/ubicacion"),
                HttpMethod.PUT,
                new HttpEntity<>(new CambiarUbicacionRequest(otraUbicacion.id()), clienteB),
                ProblemDetail.class);
        assertThat(cambiar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clienteBNoPuedeCerrarLaEstadiaDeClienteA() {
        EstadiaResponse estadiaDeA = solicitarEstadia(clienteA);
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadiaDeA.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(balneario.carperoHeaders()), EstadiaResponse.class);

        var cerrar = restTemplate.exchange(url("/api/v1/estadias/" + estadiaDeA.publicId() + "/cierre"),
                HttpMethod.POST, new HttpEntity<>(clienteB), ProblemDetail.class);
        assertThat(cerrar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Sigue ACTIVA en el historial/vigentes de A - el intento de B no la cerró.
        EstadiaResponse[] vigentesDeA = restTemplate.exchange(url("/api/v1/estadias/vigentes"), HttpMethod.GET,
                new HttpEntity<>(clienteA), EstadiaResponse[].class).getBody();
        assertThat(vigentesDeA).extracting(EstadiaResponse::publicId).contains(estadiaDeA.publicId());
    }

    // ---------------------------------------------------------------- solicitudes de servicio

    @Test
    void clienteBNoPuedeCancelarLaSolicitudDeServicioDeClienteA() {
        EstadiaResponse estadiaDeA = solicitarEstadia(clienteA);
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadiaDeA.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(balneario.carperoHeaders()), EstadiaResponse.class);
        TipoServicioResponse tipo = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.POST,
                new HttpEntity<>(new TipoServicioRequest("Hielo", true, 1), balneario.adminHeaders()),
                TipoServicioResponse.class).getBody();
        SolicitudServicioResponse solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"),
                HttpMethod.POST,
                new HttpEntity<>(new SolicitarServicioRequest(estadiaDeA.publicId(), tipo.id(), null), clienteA),
                SolicitudServicioResponse.class).getBody();

        var cancelar = restTemplate.exchange(url("/api/v1/solicitudes-servicio/" + solicitud.publicId() + "/cancelacion"),
                HttpMethod.POST, new HttpEntity<>(clienteB), ProblemDetail.class);
        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- helpers

    private EstadiaResponse solicitarEstadia(HttpHeaders clienteHeaders) {
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa " + System.nanoTime()),
                        balneario.adminHeaders()),
                UbicacionResponse.class).getBody();
        return restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(balneario.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
    }

    private String crearPedidoDeClienteA() {
        EstadiaResponse estadia = solicitarEstadia(clienteA);
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(balneario.carperoHeaders()), EstadiaResponse.class);

        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), balneario.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null, new BigDecimal("2000.00"), true, 1),
                        balneario.adminHeaders()),
                ProductoResponse.class).getBody();

        HttpHeaders conIdempotencyKey = new HttpHeaders();
        conIdempotencyKey.addAll(clienteA);
        conIdempotencyKey.set("Idempotency-Key", UUID.randomUUID().toString());
        PedidoResponse pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(new CrearPedidoRequest(estadia.publicId(),
                        List.of(new CrearPedidoRequest.ItemRequest(producto.id(), null, 1)), "fake-card-token"),
                        conIdempotencyKey),
                PedidoResponse.class).getBody();
        return pedido.publicId();
    }
}
