package com.easybeach.promotions;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.MenuCategoriaResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.web.dto.AlcancePromocionRequest;
import com.easybeach.promotions.web.dto.CambiarEstadoPromocionRequest;
import com.easybeach.promotions.web.dto.ComboItemRequest;
import com.easybeach.promotions.web.dto.PromocionRequest;
import com.easybeach.promotions.web.dto.PromocionResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Etapa 14 criterio de aceptación: cada tipo de promoción tiene tests de
 * cálculo; una promoción vencida/desactivada no aparece en el menú ni se
 * aplica a pedidos nuevos, pero los pedidos históricos conservan el
 * descuento aplicado.
 */
class PromocionCalculoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long categoriaId;
    private Long productoBebidaId;
    private Long productoComidaId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("promos");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 3"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();

        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        categoriaId = categoria.id();

        productoBebidaId = crearProducto("Gaseosa", new BigDecimal("1000.00")).id();
        productoComidaId = crearProducto("Papas fritas", new BigDecimal("2000.00")).id();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();
    }

    private ProductoResponse crearProducto(String nombre, BigDecimal precio) {
        return restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoriaId, nombre, null, precio, true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
    }

    private PromocionResponse crearPromocion(PromocionRequest request) {
        var response = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), PromocionResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private PedidoResponse pedirProducto(Long productoId, int cantidad) {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoId, null, cantidad)), "fake-card-token");
        return restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST, new HttpEntity<>(request, headers),
                PedidoResponse.class).getBody();
    }

    @Test
    void descuentoPorcentualSobreCategoriaSeAplicaAlPedido() {
        crearPromocion(new PromocionRequest("10% en bebidas", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("10"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.CATEGORIA, categoriaId)), null));

        PedidoResponse pedido = pedirProducto(productoBebidaId, 2);

        // 2 x 1000 = 2000 subtotal; 10% = 200 de descuento.
        assertThat(pedido.subtotal()).isEqualByComparingTo("2000.00");
        assertThat(pedido.descuentoTotal()).isEqualByComparingTo("200.00");
        assertThat(pedido.total()).isEqualByComparingTo("1800.00");
        assertThat(pedido.promociones()).extracting(PedidoResponse.PromocionAplicadaResponse::nombre)
                .containsExactly("10% en bebidas");
    }

    @Test
    void comboAPrecioFijoSeAplicaCuandoHayCantidadSuficiente() {
        // Combo: 1 gaseosa + 1 papas fritas por $2500 (normal: 1000+2000=3000).
        crearPromocion(new PromocionRequest("Combo gaseosa+papas", TipoPromocion.COMBO, true,
                new BigDecimal("2500"), null, null, null, null, null, null,
                List.of(new ComboItemRequest(productoBebidaId, 1), new ComboItemRequest(productoComidaId, 1))));

        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoBebidaId, null, 1),
                        new CrearPedidoRequest.ItemRequest(productoComidaId, null, 1)),
                "fake-card-token");
        PedidoResponse pedido = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(request, headers), PedidoResponse.class).getBody();

        assertThat(pedido.subtotal()).isEqualByComparingTo("3000.00");
        assertThat(pedido.descuentoTotal()).isEqualByComparingTo("500.00");
        assertThat(pedido.total()).isEqualByComparingTo("2500.00");
    }

    @Test
    void comboNoSeAplicaSiFaltaAlgunProducto() {
        crearPromocion(new PromocionRequest("Combo gaseosa+papas", TipoPromocion.COMBO, true,
                new BigDecimal("2500"), null, null, null, null, null, null,
                List.of(new ComboItemRequest(productoBebidaId, 1), new ComboItemRequest(productoComidaId, 1))));

        // Solo pide la gaseosa: el combo no completa.
        PedidoResponse pedido = pedirProducto(productoBebidaId, 1);

        assertThat(pedido.descuentoTotal()).isEqualByComparingTo("0.00");
        assertThat(pedido.total()).isEqualByComparingTo("1000.00");
    }

    @Test
    void dosPromocionesSeAcumulanSobreElMismoProducto() {
        // Decisión de negocio (etapa 14): las promociones SE ACUMULAN.
        crearPromocion(new PromocionRequest("10% en bebidas", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("10"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.CATEGORIA, categoriaId)), null));
        crearPromocion(new PromocionRequest("Happy hour gaseosa", TipoPromocion.HAPPY_HOUR, true,
                new BigDecimal("15"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoBebidaId)), null));

        PedidoResponse pedido = pedirProducto(productoBebidaId, 1);

        // 1000 subtotal; 10% (100) + 15% (150) = 250 acumulados, no "gana la mejor".
        assertThat(pedido.subtotal()).isEqualByComparingTo("1000.00");
        assertThat(pedido.descuentoTotal()).isEqualByComparingTo("250.00");
        assertThat(pedido.total()).isEqualByComparingTo("750.00");
        assertThat(pedido.promociones()).hasSize(2);
    }

    @Test
    void promocionVencidaNoApareceEnElMenuNiSeAplica() {
        crearPromocion(new PromocionRequest("Promo vieja", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("50"), LocalDate.now().minusDays(10), LocalDate.now().minusDays(1),
                null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoBebidaId)), null));

        MenuCategoriaResponse[] menu = restTemplate.getForObject(url("/api/v1/balnearios/" + ctx.slug() + "/menu"),
                MenuCategoriaResponse[].class);
        var bebida = menu[0].productos().stream().filter(p -> p.id().equals(productoBebidaId)).findFirst().orElseThrow();
        assertThat(bebida.promociones()).isEmpty();

        PedidoResponse pedido = pedirProducto(productoBebidaId, 1);
        assertThat(pedido.descuentoTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void desactivarUnaPromocionNoAlteraElDescuentoYaCongeladoEnUnPedidoHistorico() {
        PromocionResponse promo = crearPromocion(new PromocionRequest("20% en bebidas",
                TipoPromocion.DESCUENTO_PORCENTUAL, true, new BigDecimal("20"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoBebidaId)), null));

        PedidoResponse pedidoHistorico = pedirProducto(productoBebidaId, 1);
        assertThat(pedidoHistorico.descuentoTotal()).isEqualByComparingTo("200.00");

        // Se desactiva la promoción...
        restTemplate.exchange(url("/api/v1/admin/promociones/" + promo.id() + "/estado"), HttpMethod.PUT,
                new HttpEntity<>(new CambiarEstadoPromocionRequest(com.easybeach.promotions.domain.EstadoPromocion.INACTIVA),
                        ctx.adminHeaders()),
                PromocionResponse.class);

        // ...el pedido histórico conserva su descuento congelado...
        PedidoResponse relectura = restTemplate.exchange(url("/api/v1/pedidos/" + pedidoHistorico.publicId()),
                HttpMethod.GET, new HttpEntity<>(clienteHeaders), PedidoResponse.class).getBody();
        assertThat(relectura.descuentoTotal()).isEqualByComparingTo("200.00");

        // ...pero un pedido NUEVO ya no la recibe.
        PedidoResponse pedidoNuevo = pedirProducto(productoBebidaId, 1);
        assertThat(pedidoNuevo.descuentoTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void seccionDePromocionesPublicaListaLasVigentesIncluyendoCombos() {
        crearPromocion(new PromocionRequest("10% en bebidas", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("10"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.CATEGORIA, categoriaId)), null));
        crearPromocion(new PromocionRequest("Combo", TipoPromocion.COMBO, true, new BigDecimal("2500"),
                null, null, null, null, null, null,
                List.of(new ComboItemRequest(productoBebidaId, 1), new ComboItemRequest(productoComidaId, 1))));

        var promociones = restTemplate.getForObject(url("/api/v1/balnearios/" + ctx.slug() + "/promociones"),
                com.easybeach.catalog.service.PromocionesPublicasProvider.PromocionResumen[].class);
        assertThat(promociones).hasSize(2);
    }
}
