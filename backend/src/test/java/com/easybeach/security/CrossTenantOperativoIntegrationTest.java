package com.easybeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaPendienteResponse;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.RechazarEstadiaRequest;
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
 * Etapa 19 §2: la bandeja de validación de estadías del carpero (etapa 12)
 * y la cancelación de pedidos del operador (etapa 13) no tenían ninguna
 * prueba cross-tenant en el resto de la suite (confirmado por auditoría
 * previa a esta clase) — es el hueco más significativo dado el énfasis de
 * la etapa 12 en la integridad de la estadía.
 */
class CrossTenantOperativoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto a;
    private EscenarioBalneario.Contexto b;
    private HttpHeaders clienteA;

    @BeforeEach
    void seedDosBalnearios() {
        a = escenario.crearBalnearioOperativoConStaff("op-a");
        b = escenario.crearBalnearioOperativoConStaff("op-b");
        clienteA = escenario.registrarClienteYObtenerHeaders();
    }

    // ---------------------------------------------------------------- estadías

    @Test
    void carperoDeBNoVeLaEstadiaPendienteDeA() {
        String publicIdDeA = solicitarEstadia(a, clienteA);

        EstadiaPendienteResponse[] pendientesDeB = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"),
                HttpMethod.GET, new HttpEntity<>(b.carperoHeaders()), EstadiaPendienteResponse[].class).getBody();
        assertThat(pendientesDeB).extracting(EstadiaPendienteResponse::publicId).doesNotContain(publicIdDeA);
    }

    @Test
    void carperoDeBNoPuedeValidarLaEstadiaDeA() {
        String publicIdDeA = solicitarEstadia(a, clienteA);

        var validar = restTemplate.exchange(url("/api/v1/operativo/estadias/" + publicIdDeA + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(b.carperoHeaders()), ProblemDetail.class);
        assertThat(validar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Sigue PENDIENTE_VALIDACION: el intento de B no tuvo ningún efecto.
        EstadiaPendienteResponse[] pendientesDeA = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"),
                HttpMethod.GET, new HttpEntity<>(a.carperoHeaders()), EstadiaPendienteResponse[].class).getBody();
        assertThat(pendientesDeA).extracting(EstadiaPendienteResponse::publicId).contains(publicIdDeA);
    }

    @Test
    void carperoDeBNoPuedeRechazarLaEstadiaDeA() {
        String publicIdDeA = solicitarEstadia(a, clienteA);

        var rechazar = restTemplate.exchange(url("/api/v1/operativo/estadias/" + publicIdDeA + "/rechazo"),
                HttpMethod.POST, new HttpEntity<>(new RechazarEstadiaRequest("intento cross-tenant"), b.carperoHeaders()),
                ProblemDetail.class);
        assertThat(rechazar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        EstadiaPendienteResponse[] pendientesDeA = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"),
                HttpMethod.GET, new HttpEntity<>(a.carperoHeaders()), EstadiaPendienteResponse[].class).getBody();
        assertThat(pendientesDeA).extracting(EstadiaPendienteResponse::publicId).contains(publicIdDeA);
    }

    // ---------------------------------------------------------------- pedidos: cancelación

    @Test
    void operadorDeBNoPuedeCancelarPedidoDeA() {
        String publicIdPedidoDeA = crearPedidoConfirmadoEnA();

        var cancelar = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicIdPedidoDeA + "/cancelacion"),
                HttpMethod.POST,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.CANCELADO, "intento cross-tenant"),
                        b.operadorHeaders()),
                ProblemDetail.class);
        assertThat(cancelar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // El pedido de A sigue vivo en su propia cola, no cancelado.
        PedidoResponse[] colaDeA = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(a.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaDeA).extracting(PedidoResponse::publicId).contains(publicIdPedidoDeA);
        assertThat(colaDeA).filteredOn(p -> p.publicId().equals(publicIdPedidoDeA))
                .extracting(PedidoResponse::estado).doesNotContain(EstadoPedido.CANCELADO.name());
    }

    // ---------------------------------------------------------------- helpers

    private String solicitarEstadia(EscenarioBalneario.Contexto ctx, HttpHeaders clienteHeaders) {
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa " + System.nanoTime()),
                        ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        return estadia.publicId();
    }

    private String crearPedidoConfirmadoEnA() {
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa pedido " + System.nanoTime()),
                        a.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), a.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null, new BigDecimal("2000.00"), true, 1),
                        a.adminHeaders()),
                ProductoResponse.class).getBody();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(a.slug(), ubicacion.id()), clienteA), EstadiaResponse.class)
                .getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(a.carperoHeaders()), EstadiaResponse.class);

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
