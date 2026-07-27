package com.easybeach.promotions;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.promotions.domain.TipoAlcance;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.promotions.web.dto.AlcancePromocionRequest;
import com.easybeach.promotions.web.dto.ComboItemRequest;
import com.easybeach.promotions.web.dto.PromocionRequest;
import com.easybeach.promotions.web.dto.PromocionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class PromocionAbmIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private Long productoId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("promos-abm");
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null, new BigDecimal("1000.00"),
                        true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();
    }

    @Test
    void descuentoPorcentualSinAlcanceEsRechazado() {
        var request = new PromocionRequest("Sin alcance", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("10"), null, null, null, null, null, null, null);
        var response = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void comboConMenosDeDosProductosEsRechazado() {
        var request = new PromocionRequest("Combo incompleto", TipoPromocion.COMBO, true, new BigDecimal("100"),
                null, null, null, null, null, null, List.of(new ComboItemRequest(productoId, 1)));
        var response = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void vigenciaDesdePosteriorAHastaEsRechazada() {
        var request = new PromocionRequest("Fechas invertidas", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("10"), LocalDate.now().plusDays(10), LocalDate.now(), null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoId)), null);
        var response = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void productoDeOtroBalnearioComoAlcanceEsRechazado() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("promos-ajeno");
        CategoriaMenuResponse categoriaAjena = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Otra", 1, true), otro.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse productoAjeno = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoriaAjena.id(), "Ajeno", null, new BigDecimal("1.00"),
                        true, 1), otro.adminHeaders()),
                ProductoResponse.class).getBody();

        var request = new PromocionRequest("Cruzada", TipoPromocion.DESCUENTO_PORCENTUAL, true, new BigDecimal("10"),
                null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoAjeno.id())), null);
        var response = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void adminPuedeCrearListarYBorrarUnaPromocion() {
        var request = new PromocionRequest("Promo simple", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                new BigDecimal("15"), null, null, null, null, null,
                List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoId)), null);
        PromocionResponse creada = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(request, ctx.adminHeaders()), PromocionResponse.class).getBody();
        assertThat(creada.alcances()).hasSize(1);

        PromocionResponse[] listado = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), PromocionResponse[].class).getBody();
        assertThat(listado).extracting(PromocionResponse::id).contains(creada.id());

        var eliminar = restTemplate.exchange(url("/api/v1/admin/promociones/" + creada.id()), HttpMethod.DELETE,
                new HttpEntity<>(ctx.adminHeaders()), Void.class);
        assertThat(eliminar.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        PromocionResponse[] listadoTrasBorrar = restTemplate.exchange(url("/api/v1/admin/promociones"),
                HttpMethod.GET, new HttpEntity<>(ctx.adminHeaders()), PromocionResponse[].class).getBody();
        assertThat(listadoTrasBorrar).extracting(PromocionResponse::id).doesNotContain(creada.id());
    }

    @Test
    void adminDeOtroBalnearioNoPuedeEditarPromocionAjena() {
        PromocionResponse creada = restTemplate.exchange(url("/api/v1/admin/promociones"), HttpMethod.POST,
                new HttpEntity<>(new PromocionRequest("Mía", TipoPromocion.DESCUENTO_PORCENTUAL, true,
                        new BigDecimal("10"), null, null, null, null, null,
                        List.of(new AlcancePromocionRequest(TipoAlcance.PRODUCTO, productoId)), null),
                        ctx.adminHeaders()),
                PromocionResponse.class).getBody();

        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("promos-intruso");
        var intento = restTemplate.exchange(url("/api/v1/admin/promociones/" + creada.id() + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new com.easybeach.promotions.web.dto.CambiarEstadoPromocionRequest(
                        com.easybeach.promotions.domain.EstadoPromocion.INACTIVA), otro.adminHeaders()),
                ProblemDetail.class);
        assertThat(intento.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
