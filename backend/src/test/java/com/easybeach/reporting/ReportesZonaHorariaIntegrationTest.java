package com.easybeach.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.reporting.dto.VentasReporteResponse;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Etapa 15 criterio de aceptación: "filtros de fecha respetan la zona
 * horaria de la etapa 04" (Argentina, UTC-3). Un pedido creado a las 02:00
 * UTC del día D+1 cae en el día D en hora argentina (23:00 del día D) - si
 * el reporte agrupara por la fecha UTC cruda, lo pondría en el día
 * equivocado.
 */
class ReportesZonaHorariaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void unPedidoCercaDeMedianocheSeAgrupaPorElDiaEnHoraArgentinaNoUtc() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("tz");
        HttpHeaders clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Agua", null, new BigDecimal("500.00"),
                        true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadia.publicId(),
                List.of(new CrearPedidoRequest.ItemRequest(producto.id(), null, 1)), "fake-card-token");
        String pedidoPublicId = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(request, headers), PedidoResponse.class).getBody().publicId();
        for (EstadoPedido destino : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO)) {
            restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedidoPublicId + "/estado"), HttpMethod.PUT,
                    new HttpEntity<>(new TransicionPedidoRequest(destino, null), ctx.operadorHeaders()),
                    PedidoResponse.class);
        }

        // 2026-01-16 02:00:00 UTC = 2026-01-15 23:00:00 en Argentina (UTC-3):
        // "hoy" para el reporte tiene que ser el 15, no el 16.
        LocalDate diaUtc = LocalDate.of(2026, 1, 16);
        LocalDate diaArgentina = LocalDate.of(2026, 1, 15);
        // Bind como STRING, no como Timestamp/LocalDateTime: DATETIME en MySQL
        // no tiene timezone propia, pero el DRIVER JDBC sí puede reinterpretar
        // un objeto Timestamp/LocalDateTime según su propia TZ de sesión. Un
        // literal de texto no sufre esa conversión - se escribe tal cual.
        jdbc.update("update pedido set created_at = ? where public_id = ?",
                "2026-01-16 02:00:00", pedidoPublicId);

        VentasReporteResponse reporteDiaArgentina = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas?desde=" + diaArgentina + "&hasta=" + diaArgentina), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), VentasReporteResponse.class).getBody();
        VentasReporteResponse reporteDiaUtc = restTemplate.exchange(
                url("/api/v1/admin/reportes/ventas?desde=" + diaUtc + "&hasta=" + diaUtc), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), VentasReporteResponse.class).getBody();

        assertThat(reporteDiaArgentina.cantidadPedidos()).as("cae en el día argentino (15)").isEqualTo(1);
        assertThat(reporteDiaArgentina.facturacionTotal()).isEqualByComparingTo("500.00");
        assertThat(reporteDiaUtc.cantidadPedidos()).as("NO cae en el día UTC crudo (16)").isZero();

        assertThat(reporteDiaArgentina.porDia()).singleElement()
                .satisfies(d -> assertThat(d.dia()).isEqualTo(diaArgentina));
    }
}
