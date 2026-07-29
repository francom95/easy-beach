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
import com.easybeach.payments.service.PagoReconciliacionJob;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import com.easybeach.support.FakeMercadoPagoPaymentClient;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Job de reconciliación de pagos (ADR-004). Cubre el agujero que encontró la
 * revisión de cierre: la query y el cliente HTTP existían, pero nada los
 * llamaba, así que un webhook perdido dejaba al cliente pagado y al pedido
 * fuera de la cola de la cocina para siempre.
 */
class PagoReconciliacionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private FakeMercadoPagoPaymentClient.Control controlPagos;
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private PedidoPagoRepository pagoRepository;
    @Autowired
    private PagoReconciliacionJob job;
    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long productoId;

    @BeforeEach
    void seed() {
        controlPagos.reset();
        ctx = escenario.crearBalnearioOperativoConStaff("reconciliacion");
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

    /**
     * El job solo mira pagos con cierta antigüedad. {@code created_at} viene de
     * {@code Auditable} y no tiene setter, así que se envejece por SQL directo
     * (mismo recurso que usa el seeder de demo para los datos históricos).
     */
    private void envejecerPagoDe(String pedidoPublicId) {
        var pedido = pedidoRepository.findByPublicId(pedidoPublicId).orElseThrow();
        Long pagoId = pagoRepository.findByPedidoIdOrderByIdAsc(pedido.getId()).get(0).getId();
        Instant viejo = Instant.now().minus(Duration.ofMinutes(PagoReconciliacionJob.MARGEN_MINUTOS + 5));
        jdbc.update("update pedido_pago set created_at = ? where id = ?", Timestamp.from(viejo), pagoId);
    }

    @Test
    void unPagoAprobadoEnMpCuyoWebhookNuncaLlegoSeRescataYEntraALaCola() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        assertThat(pedido.estado()).isEqualTo(EstadoPedido.PAGO_PENDIENTE.name());

        // MP lo aprobó, pero el webhook nunca llegó: nadie se enteró.
        String mpPaymentId = mpPaymentIdDe(pedido.publicId());
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.APPROVED);
        envejecerPagoDe(pedido.publicId());

        PedidoResponse[] colaAntes = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaAntes).extracting(PedidoResponse::publicId).doesNotContain(pedido.publicId());

        job.conciliarPendientes();

        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.APROBADO);
        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CONFIRMADO);

        PedidoResponse[] colaDespues = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), PedidoResponse[].class).getBody();
        assertThat(colaDespues).extracting(PedidoResponse::publicId).contains(pedido.publicId());
    }

    @Test
    void unPagoRechazadoEnMpSeReflejaSinConfirmarElPedido() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        String mpPaymentId = mpPaymentIdDe(pedido.publicId());
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.REJECTED);
        envejecerPagoDe(pedido.publicId());

        job.conciliarPendientes();

        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGO_RECHAZADO);
    }

    @Test
    void unPagoQueSigueePendienteEnMpQuedaIgualParaElProximoCiclo() {
        PedidoResponse pedido = crearPedidoPendienteDePago();
        String mpPaymentId = mpPaymentIdDe(pedido.publicId());
        controlPagos.definirEstadoDe(mpPaymentId, MercadoPagoPaymentClient.EstadoMp.PENDING);
        envejecerPagoDe(pedido.publicId());

        job.conciliarPendientes();

        assertThat(pagoRepository.findByMpPaymentId(mpPaymentId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pedidoRepository.findByPublicId(pedido.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGO_PENDIENTE);
    }

    /**
     * La razón por la que cada pago va en su propia transacción: los tokens son
     * por balneario, así que una cuenta de MP caída no puede dejar sin conciliar
     * a los pedidos de los demás balnearios.
     */
    @Test
    void unPagoQueFallaContraMpNoImpideConciliarElResto() {
        PedidoResponse conFalla = crearPedidoPendienteDePago();
        String idConFalla = mpPaymentIdDe(conFalla.publicId());
        controlPagos.fallarConsultaDe(idConFalla);
        envejecerPagoDe(conFalla.publicId());

        PedidoResponse sano = crearPedidoPendienteDePago();
        String idSano = mpPaymentIdDe(sano.publicId());
        controlPagos.definirEstadoDe(idSano, MercadoPagoPaymentClient.EstadoMp.APPROVED);
        envejecerPagoDe(sano.publicId());

        job.conciliarPendientes();

        // El que falló sigue pendiente (se reintenta el próximo ciclo)...
        assertThat(pagoRepository.findByMpPaymentId(idConFalla).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.PENDIENTE);
        // ...pero el sano se concilió igual.
        assertThat(pagoRepository.findByMpPaymentId(idSano).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.APROBADO);
        assertThat(pedidoRepository.findByPublicId(sano.publicId()).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CONFIRMADO);
    }
}
