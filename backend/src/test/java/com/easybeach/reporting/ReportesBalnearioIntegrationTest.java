package com.easybeach.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.concierge.domain.EstadoSolicitudServicio;
import com.easybeach.concierge.web.dto.SolicitarServicioRequest;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.concierge.web.dto.TransicionSolicitudRequest;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.web.dto.AlcancePromocionRequest;
import com.easybeach.promotions.web.dto.PromocionRequest;
import com.easybeach.promotions.web.dto.PromocionResponse;
import com.easybeach.reporting.dto.DashboardResumenResponse;
import com.easybeach.reporting.dto.EstadiasReporteResponse;
import com.easybeach.reporting.dto.ProductoVendidoResponse;
import com.easybeach.reporting.dto.PromocionRendimientoResponse;
import com.easybeach.reporting.dto.ServiciosReporteResponse;
import com.easybeach.reporting.dto.VentasReporteResponse;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Etapa 15: cada reporte con un dataset conocido, verificado a mano. Dataset
 * de este test: 2 pedidos ENTREGADO (2000 + 1000 = 3000 subtotal, con 10% de
 * descuento por categoría cada uno) y 1 pedido CANCELADO (que no debe
 * contar en ningún lado).
 */
class ReportesBalnearioIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long productoId;
    private LocalDate hoy;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("reportes");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();
        hoy = LocalDate.now(com.easybeach.shared.time.ZonaNegocio.ZONE_ID);

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null, new BigDecimal("1000.00"),
                        true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();

        // 10% de descuento en toda la categoría: aplica a los 3 pedidos, pero
        // el reporte solo debe contar los ENTREGADO.
        restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(new PromocionRequest("10% bebidas", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                        new BigDecimal("10"), null, null, null, null, null,
                        List.of(new AlcancePromocionRequest(TipoAlcance.CATEGORIA, categoria.id())), null),
                        ctx.adminHeaders()),
                PromocionResponse.class);

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();

        // Pedido 1: 2 unidades -> 2000 subtotal, 200 descuento, 1800 total. ENTREGADO.
        String p1 = crearYEntregar(2);
        // Pedido 2: 1 unidad -> 1000 subtotal, 100 descuento, 900 total. ENTREGADO.
        String p2 = crearYEntregar(1);
        // Pedido 3: 1 unidad -> se cancela. NO debe aparecer en ningún reporte financiero.
        cancelarPedido(crearPedido(1));

        // Servicio al carpero: se solicita y se resuelve, con tiempo de
        // resolución controlado a mano (30 minutos) vía UPDATE directo.
        TipoServicioResponse tipo = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.POST,
                new HttpEntity<>(new TipoServicioRequest("Hielo", true, 1), ctx.adminHeaders()),
                TipoServicioResponse.class).getBody();
        SolicitudServicioResponse solicitud = restTemplate.exchange(url("/api/v1/solicitudes-servicio"),
                HttpMethod.POST, new HttpEntity<>(new SolicitarServicioRequest(estadiaPublicId, tipo.id(), null),
                        clienteHeaders),
                SolicitudServicioResponse.class).getBody();
        // Literal UTC, no java.sql.Timestamp: un Timestamp bindeado por JDBC
        // plano usa la zona horaria de la JVM (America/Buenos_Aires, UTC-3
        // acá), no la UTC que Hibernate fuerza vía hibernate.jdbc.time_zone -
        // mismo bug real que en los repositorios de reporting (ver su Javadoc).
        String hace30Minutos = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC)
                .format(Instant.now().minusSeconds(30 * 60));
        jdbc.update("update solicitud_servicio set created_at = ? where public_id = ?",
                hace30Minutos, solicitud.publicId());
        // El ciclo es PENDIENTE -> EN_CURSO -> RESUELTA (etapa 14): saltar
        // directo a RESUELTA es una transición inválida (409) que dejaría la
        // solicitud en PENDIENTE para siempre.
        restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + solicitud.publicId() + "/estado"),
                HttpMethod.PUT, new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.EN_CURSO),
                        ctx.carperoHeaders()),
                SolicitudServicioResponse.class);
        restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/" + solicitud.publicId() + "/estado"),
                HttpMethod.PUT, new HttpEntity<>(new TransicionSolicitudRequest(EstadoSolicitudServicio.RESUELTA),
                        ctx.carperoHeaders()),
                SolicitudServicioResponse.class);
    }

    private String crearPedido(int cantidad) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoId, null, cantidad)), "fake-card-token");
        return restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST, new HttpEntity<>(request, headers),
                PedidoResponse.class).getBody().publicId();
    }

    private String crearYEntregar(int cantidad) {
        String publicId = crearPedido(cantidad);
        for (EstadoPedido destino : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO)) {
            restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicId + "/estado"), HttpMethod.PUT,
                    new HttpEntity<>(new TransicionPedidoRequest(destino, null), ctx.operadorHeaders()),
                    PedidoResponse.class);
        }
        return publicId;
    }

    private void cancelarPedido(String publicId) {
        restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicId + "/cancelacion"), HttpMethod.POST,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.CANCELADO, "Test: no debe contar"),
                        ctx.operadorHeaders()),
                PedidoResponse.class);
    }

    @Test
    void reporteDeVentasCuentaSoloLoEntregado() {
        VentasReporteResponse reporte = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), VentasReporteResponse.class).getBody();

        // (2000-200) + (1000-100) = 2700. El pedido cancelado (900) no cuenta.
        assertThat(reporte.facturacionTotal()).isEqualByComparingTo("2700.00");
        assertThat(reporte.cantidadPedidos()).isEqualTo(2);
        assertThat(reporte.ticketPromedio()).isEqualByComparingTo("1350.00");
        assertThat(reporte.porDia()).singleElement().satisfies(d -> {
            assertThat(d.dia()).isEqualTo(hoy);
            assertThat(d.cantidadPedidos()).isEqualTo(2);
            assertThat(d.facturacion()).isEqualByComparingTo("2700.00");
        });
    }

    @Test
    void reporteDeVentasCsvSeDescargaComoArchivo() {
        var response = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas/csv?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("ventas.csv");
        assertThat(response.getBody()).contains("dia,cantidadPedidos,facturacion").contains("2700.00");
    }

    @Test
    void productosMasVendidosSoloCuentaLoEntregado() {
        List<ProductoVendidoResponse> productos = List.of(restTemplate.exchange(
                url("/api/v1/admin/reportes/productos-mas-vendidos?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ProductoVendidoResponse[].class).getBody());

        assertThat(productos).singleElement().satisfies(p -> {
            assertThat(p.productoId()).isEqualTo(productoId);
            // 2 (pedido 1) + 1 (pedido 2) = 3 unidades; el pedido cancelado (1) no cuenta.
            assertThat(p.unidadesVendidas()).isEqualTo(3);
            // Facturación de pedido_item es SIN descuento (subtotal_linea): 2000 + 1000 = 3000.
            assertThat(p.facturacion()).isEqualByComparingTo("3000.00");
        });
    }

    @Test
    void rendimientoDePromocionesSoloCuentaLoEntregado() {
        List<PromocionRendimientoResponse> promos = List.of(restTemplate.exchange(
                url("/api/v1/admin/reportes/promociones?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), PromocionRendimientoResponse[].class).getBody());

        assertThat(promos).singleElement().satisfies(p -> {
            assertThat(p.nombrePromocion()).isEqualTo("10% bebidas");
            // 2 usos (pedido 1 y 2); el pedido 3 (cancelado) también tenía la
            // promo aplicada pero no debe contarse acá.
            assertThat(p.usos()).isEqualTo(2);
            assertThat(p.montoDescontado()).isEqualByComparingTo("300.00");
        });
    }

    @Test
    void reporteDeEstadiasCalculaAperturasDuracionYConsumo() {
        // Solo este test necesita la estadía CERRADA (fecha_cierre real para
        // duración/consumo promedio); cerrarla en seed() rompería los otros
        // tests que siguen creando pedidos sobre la misma estadía.
        restTemplate.exchange(url("/api/v1/estadias/" + estadiaPublicId + "/cierre"), HttpMethod.POST,
                new HttpEntity<>(clienteHeaders), Object.class);

        EstadiasReporteResponse reporte = restTemplate.exchange(
                url("/api/v1/admin/reportes/estadias?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), EstadiasReporteResponse.class).getBody();

        assertThat(reporte.aperturasPorDia()).singleElement().satisfies(a -> {
            assertThat(a.dia()).isEqualTo(hoy);
            assertThat(a.cantidad()).isEqualTo(1);
        });
        assertThat(reporte.duracionPromedioHoras()).isNotNull();
        // Consumo ENTREGADO de la única estadía: 1800 + 900 = 2700 (el cancelado no cuenta).
        assertThat(reporte.consumoPromedioPorEstadia()).isEqualByComparingTo("2700.00");
    }

    @Test
    void reporteDeServiciosCuentaPorTipoYTiempoDeResolucion() {
        ServiciosReporteResponse reporte = restTemplate.exchange(
                url("/api/v1/admin/reportes/servicios?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ServiciosReporteResponse.class).getBody();

        assertThat(reporte.porTipo()).singleElement().satisfies(t -> {
            assertThat(t.tipoServicio()).isEqualTo("Hielo");
            assertThat(t.cantidad()).isEqualTo(1);
        });
        // Se forzó created_at a 30 min antes de resolverla; tolerancia por el
        // tiempo real que tarda el test en ejecutar el paso intermedio.
        assertThat(reporte.tiempoResolucionPromedioMinutos()).isBetween(29.0, 33.0);
    }

    @Test
    void dashboardMuestraKpisDelDiaYPedidosEnCurso() {
        // Un cuarto pedido que se queda EN_PREPARACION: cuenta como "en curso"
        // pero no como facturación (todavía no se entregó).
        String enCurso = crearPedido(1);
        restTemplate.exchange(url("/api/v1/operativo/pedidos/" + enCurso + "/estado"), HttpMethod.PUT,
                new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.EN_PREPARACION, null), ctx.operadorHeaders()),
                PedidoResponse.class);

        DashboardResumenResponse dashboard = restTemplate.exchange(url("/api/v1/admin/reportes/dashboard"),
                HttpMethod.GET, new HttpEntity<>(ctx.adminHeaders()), DashboardResumenResponse.class).getBody();

        assertThat(dashboard.facturacionHoy()).isEqualByComparingTo("2700.00");
        assertThat(dashboard.pedidosHoy()).isEqualTo(2);
        assertThat(dashboard.ticketPromedioHoy()).isEqualByComparingTo("1350.00");
        assertThat(dashboard.pedidosEnCurso()).isEqualTo(1);
    }

    @Test
    void unAdminDeOtroBalnearioNoVeNadaDeEsteEnSusReportes() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("reportes-ajeno");

        VentasReporteResponse reporteAjeno = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(otro.adminHeaders()), VentasReporteResponse.class).getBody();

        assertThat(reporteAjeno.facturacionTotal()).isEqualByComparingTo("0.00");
        assertThat(reporteAjeno.cantidadPedidos()).isZero();
    }

    /**
     * Etapa 19 §2: todos los endpoints de este controlador resuelven el
     * balneario exclusivamente desde {@code principal.balnearioId()} - no
     * toman ningún id de path/query manipulable, así que no hay superficie
     * de IDOR distinta entre ellos. {@code unAdminDeOtroBalnearioNoVeNadaDeEsteEnSusReportes}
     * ya prueba que el filtro por tenant funciona contra {@code /ventas};
     * estos dos casos agregan cobertura donde SÍ hay una diferencia real de
     * código: {@code dashboard} usa un método de servicio distinto (sin
     * rango de fechas) y {@code /csv} pasa por un {@code CsvWriter} que
     * podría, en teoría, filtrar distinto que su contraparte JSON. Los otros
     * 4 endpoints (productos-mas-vendidos, promociones, estadias, servicios
     * y sus /csv) comparten exactamente el mismo mecanismo de filtro que
     * /ventas vía {@code ReporteBalnearioService} y no se replican acá.
     */
    @Test
    void unAdminDeOtroBalnearioNoVeElDashboardDeEste() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("reportes-ajeno-dashboard");

        DashboardResumenResponse dashboardAjeno = restTemplate.exchange(url("/api/v1/admin/reportes/dashboard"),
                HttpMethod.GET, new HttpEntity<>(otro.adminHeaders()), DashboardResumenResponse.class).getBody();

        assertThat(dashboardAjeno.facturacionHoy()).isEqualByComparingTo("0.00");
        assertThat(dashboardAjeno.pedidosHoy()).isZero();
        assertThat(dashboardAjeno.pedidosEnCurso()).isZero();
    }

    @Test
    void unAdminDeOtroBalnearioNoVeNadaEnElCsvDeVentas() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("reportes-ajeno-csv");

        var response = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas/csv?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(otro.adminHeaders()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("dia,cantidadPedidos,facturacion\r\n");
    }

    @Test
    void unOperadorNoPuedeConsultarReportes() {
        var response = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas?desde=" + hoy + "&hasta=" + hoy), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
