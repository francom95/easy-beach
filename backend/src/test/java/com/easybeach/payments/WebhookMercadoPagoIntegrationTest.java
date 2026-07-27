package com.easybeach.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.repository.PedidoRepository;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.payments.domain.EstadoPago;
import com.easybeach.payments.repository.PedidoPagoRepository;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
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
import org.springframework.http.MediaType;

/**
 * Webhook de Mercado Pago (ADR-004 / etapa 05 §4.1): idempotencia, orden y
 * el hecho de que el body NO es fuente de verdad.
 */
class WebhookMercadoPagoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private FakeMercadoPagoPaymentClient.Control controlPagos;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoPagoRepository pagoRepository;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long productoId;

    @BeforeEach
    void seed() {
        controlPagos.reset();
        ctx = escenario.crearBalnearioOperativoConStaff("webhook");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Comidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Hamburguesa", null,
                        new BigDecimal("8000.00"), true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();
    }

    /** Crea un pedido cuyo pago queda PENDIENTE, esperando resolución por webhook. */
    private PedidoResponse crearPedidoPendienteDePago() {
        controlPagos.dejarProximoPendiente();
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        return restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(new CrearPedidoRequest(estadiaPublicId,
                        List.of(new CrearPedidoRequest.ItemRequest(productoId, null, 1)), "fake-card-token"), headers),
                PedidoResponse.class).getBody();
    }

    private String mpPaymentIdDe(String pedidoPublicId) {
        var pedido = pedidoRepository.findByPublicId(pedidoPublicId).orElseThrow();
        return pagoRepository.findByPedidoIdOrderByIdAsc(pedido.getId()).get(0).getMpPaymentId();
    }

    private HttpEntity<String> webhookDe(String mpPaymentId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Sin secret configurado en tests, la verificación de firma no bloquea
        // (ver WebhookSignatureVerifier); el test de firma inválida usa uno propio.
        return new HttpEntity<>("{\"type\":\"payment\",\"data\":{\"id\":\"" + mpPaymentId + "\"}}", headers);
    }

    @Test
    void webhookAprobadoConfirmaElPedidoYLoMeteEnLaCola() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.PAGO_PENDIENTE.name());

        // Antes del webhook, la cocina no lo ve.
        PedidoResponse[] colaAntes = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaAntes).extracting(PedidoResponse::publicId).doesNotContain(pedido.publicId());

        String mpPaymentId = mpPaymentIdDe(pedido.publicId());
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.APPROVED);

        var response = restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST,
                webhookDe(mpPaymentId), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CONFIRMADO);

        PedidoResponse[] colaDespues = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaDespues).extracting(PedidoResponse::publicId).contains(pedido.publicId());
    }

    @Test
    void webhookDuplicadoNoReProcesa() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        String mpPaymentId = mpPaymentIdDe(pedido.publicId());
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.APPROVED);

        restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST, webhookDe(mpPaymentId), Void.class);
        var segunda = restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST,
                webhookDe(mpPaymentId), Void.class);

        // Responde 200 igual (MP no debe reintentar), pero el pedido no se movió
        // de nuevo ni se duplicó ningún evento de confirmación.
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CONFIRMADO);
        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.APROBADO);
    }

    @Test
    void webhookDePagoDesconocidoNoRompe() {
        var response = restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST,
                webhookDe("pago-que-no-existe-" + System.nanoTime()), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void webhookSinDataIdEsRechazado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST,
                new HttpEntity<>("{\"type\":\"payment\"}", headers), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unPagoAprobadoNoVuelveAtrasPorUnaNotificacionFueraDeOrden() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        String mpPaymentId = mpPaymentIdDe(pedido.publicId());

        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.APPROVED);
        restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST, webhookDe(mpPaymentId), Void.class);
        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.APROBADO);

        // Llega tarde una notificación que dice "pending": no debe degradar el pago.
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.PENDING);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(url("/api/v1/mercadopago/webhook"), HttpMethod.POST,
                new HttpEntity<>("{\"type\":\"payment\",\"data\":{\"id\":\"" + mpPaymentId + "\"},\"v\":2}", headers),
                Void.class);

        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.APROBADO);
        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CONFIRMADO);
    }
}
