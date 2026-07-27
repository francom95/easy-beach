package com.easybeach.ordering;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.catalog.web.dto.ProductoVarianteRequest;
import com.easybeach.catalog.web.dto.ProductoVarianteResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoEventoResponse;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.ResumenCierreResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import com.easybeach.support.FakeMercadoPagoPaymentClient;
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

/** Flujo de punta a punta del núcleo transaccional (etapa 13). */
class PedidoFlujoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private FakeMercadoPagoPaymentClient.Control controlPagos;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long productoId;
    private Long varianteId;

    @BeforeEach
    void seed() {
        controlPagos.reset();
        ctx = escenario.crearBalnearioOperativoConStaff("pedidos");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 7"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();

        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null,
                        new BigDecimal("2000.00"), true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();
        ProductoVarianteResponse variante = restTemplate.exchange(
                url("/api/v1/admin/productos/" + productoId + "/variantes"), HttpMethod.POST,
                new HttpEntity<>(new ProductoVarianteRequest("Grande", new BigDecimal("2500.00"), true, 1),
                        ctx.adminHeaders()),
                ProductoVarianteResponse.class).getBody();
        varianteId = variante.id();

        // Estadía ACTIVA: sin esto no se puede pedir.
        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();
    }

    private HttpHeaders conIdempotencyKey(HttpHeaders base, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(base);
        headers.set("Idempotency-Key", key);
        return headers;
    }

    private CrearPedidoRequest pedidoDe(int cantidad, Long variante) {
        return new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoId, variante, cantidad)), "fake-card-token");
    }

    @Test
    void flujoCompletoCrearConfirmarPrepararEntregar() {
        var response = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(2, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PedidoResponse pedido = response.getBody();

        // Precio de la VARIANTE (2500), no el base del producto (2000).
        assertThat(pedido.subtotal()).isEqualByComparingTo("5000.00");
        assertThat(pedido.total()).isEqualByComparingTo("5000.00");
        assertThat(pedido.items()).singleElement()
                .satisfies(i -> {
                    assertThat(i.nombreProducto()).isEqualTo("Gaseosa");
                    assertThat(i.nombreVariante()).isEqualTo("Grande");
                    assertThat(i.precioUnitario()).isEqualByComparingTo("2500.00");
                });
        // El fake aprueba sincrónicamente: el pedido ya quedó CONFIRMADO.
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMADO.name());

        // Aparece en la cola operativa (solo entra con pago aprobado).
        PedidoResponse[] cola = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(cola).extracting(PedidoResponse::publicId).contains(pedido.publicId());

        // Despacho completo.
        for (EstadoPedido destino : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO,
                EstadoPedido.ENTREGADO)) {
            var paso = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/estado"),
                    HttpMethod.PUT,
                    new HttpEntity<>(new TransicionPedidoRequest(destino, null), ctx.operadorHeaders()),
                    PedidoResponse.class);
            assertThat(paso.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(paso.getBody().estado()).isEqualTo(destino.name());
        }

        // Toda transición quedó en el historial.
        PedidoEventoResponse[] historial = restTemplate.exchange(
                url("/api/v1/pedidos/" + pedido.publicId() + "/historial"), HttpMethod.GET,
                new HttpEntity<>(clienteHeaders), PedidoEventoResponse[].class).getBody();
        assertThat(historial).extracting(PedidoEventoResponse::estadoNuevo)
                .containsSubsequence("CREADO", "PAGO_PENDIENTE", "CONFIRMADO",
                        "EN_PREPARACION", "EN_CAMINO", "ENTREGADO");

        // Entregado: ya no está en la cola.
        PedidoResponse[] colaFinal = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaFinal).extracting(PedidoResponse::publicId).doesNotContain(pedido.publicId());
    }

    @Test
    void idempotenciaMismaClaveDosVecesUnSoloPedidoYUnSoloCobro() {
        String clave = UUID.randomUUID().toString();
        var primera = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, clave)),
                PedidoResponse.class);
        var segunda = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, clave)),
                PedidoResponse.class);

        assertThat(segunda.getBody().publicId()).isEqualTo(primera.getBody().publicId());
        assertThat(controlPagos.cobrosRealizados()).as("cobros disparados").isEqualTo(1);
    }

    @Test
    void conVariantesDisponiblesElegirUnaEsObligatorio() {
        var response = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(3, null), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * Criterio de aceptación "test de manipulación": el contrato de entrada ni
     * siquiera tiene campo de precio, así que la única fuente posible del
     * total es el catálogo del servidor. Se verifica contra el precio real.
     */
    @Test
    void elTotalSaleDelCatalogoDelServidor() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(4, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();

        // 4 × 2500 (precio de la variante en el catálogo) = 10000, sin importar
        // qué mande el cliente.
        assertThat(pedido.subtotal()).isEqualByComparingTo("10000.00");
        assertThat(pedido.total()).isEqualByComparingTo("10000.00");
    }

    @Test
    void pedidoConProductoDeOtroBalnearioEsRechazado() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("ajeno");
        CategoriaMenuResponse categoriaAjena = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Ajena", 1, true), otro.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse productoAjeno = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoriaAjena.id(), "Ajeno", null,
                        new BigDecimal("100.00"), true, 1), otro.adminHeaders()),
                ProductoResponse.class).getBody();

        var request = new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoAjeno.id(), null, 1)), "fake-card-token");
        var response = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(request, conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void pagoRechazadoNoEntraALaColaOperativa() {
        controlPagos.rechazarProximoPago();
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();

        assertThat(pedido.estado()).isEqualTo(EstadoPedido.PAGO_RECHAZADO.name());

        PedidoResponse[] cola = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(cola).extracting(PedidoResponse::publicId).doesNotContain(pedido.publicId());
    }

    @Test
    void cancelarUnPedidoYaPagadoDisparaReembolso() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.CONFIRMADO.name());

        var cancelado = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/cancelacion"),
                HttpMethod.POST,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.CANCELADO, "Se acabó el stock"),
                        ctx.operadorHeaders()),
                PedidoResponse.class);

        assertThat(cancelado.getBody().estado()).isEqualTo(EstadoPedido.CANCELADO.name());
        assertThat(controlPagos.reembolsosRealizados()).isEqualTo(1);
    }

    @Test
    void cancelarSinMotivoEsRechazado() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();

        var sinMotivo = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/cancelacion"),
                HttpMethod.POST,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.CANCELADO, "  "), ctx.operadorHeaders()),
                ProblemDetail.class);
        assertThat(sinMotivo.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void transicionInvalidaEsRechazada() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();

        // CONFIRMADO no puede saltar directo a ENTREGADO.
        var salto = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.ENTREGADO, null), ctx.operadorHeaders()),
                ProblemDetail.class);
        assertThat(salto.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void unPedidoConfirmadoBloqueaElCierreDeLaEstadia() {
        restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class);

        // Cierra el círculo de la etapa 12: ConsumoEstadiaProvider ahora tiene
        // implementación real y el cierre se bloquea de verdad.
        var cierre = restTemplate.exchange(url("/api/v1/estadias/" + estadiaPublicId + "/cierre"),
                HttpMethod.POST, new HttpEntity<>(clienteHeaders), ProblemDetail.class);
        assertThat(cierre.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void alEntregarTodoElResumenDeCierreCuadraConLoConsumido() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(2, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();
        for (EstadoPedido destino : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO,
                EstadoPedido.ENTREGADO)) {
            restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/estado"), HttpMethod.PUT,
                    new HttpEntity<>(new TransicionPedidoRequest(destino, null), ctx.operadorHeaders()),
                    PedidoResponse.class);
        }

        ResumenCierreResponse resumen = restTemplate.exchange(url("/api/v1/estadias/" + estadiaPublicId + "/cierre"),
                HttpMethod.POST, new HttpEntity<>(clienteHeaders), ResumenCierreResponse.class).getBody();

        assertThat(resumen.cantidadPedidos()).isEqualTo(1);
        assertThat(resumen.montoTotal()).isEqualByComparingTo("5000.00");
    }

    @Test
    void operadorDeOtroBalnearioNoVeNiTransicionaPedidosAjenos() {
        var pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(pedidoDe(1, varianteId), conIdempotencyKey(clienteHeaders, UUID.randomUUID().toString())),
                PedidoResponse.class).getBody();

        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("intruso");

        PedidoResponse[] colaAjena = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(otro.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaAjena).extracting(PedidoResponse::publicId).doesNotContain(pedido.publicId());

        var intento = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedido.publicId() + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.EN_PREPARACION, null), otro.operadorHeaders()),
                ProblemDetail.class);
        assertThat(intento.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void noSePuedePedirDesdeUnaEstadiaQueNoEstaActiva() {
        // Estadía nueva sin validar del carpero.
        UbicacionResponse otraUbicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.MESA, "Mesa 9"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        HttpHeaders otroCliente = escenario.registrarClienteYObtenerHeaders();
        EstadiaResponse pendiente = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), otraUbicacion.id()), otroCliente),
                EstadiaResponse.class).getBody();

        var request = new CrearPedidoRequest(pendiente.publicId(),
                List.of(new CrearPedidoRequest.ItemRequest(productoId, varianteId, 1)), "fake-card-token");
        var response = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(request, conIdempotencyKey(otroCliente, UUID.randomUUID().toString())),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
