package com.easybeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.branding.theming.TypographyFamily;
import com.easybeach.branding.web.dto.BrandingUpdateRequest;
import com.easybeach.branding.web.dto.BrandingUpdateResult;
import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.DisponibilidadRequest;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.catalog.web.dto.ProductoVarianteRequest;
import com.easybeach.catalog.web.dto.ProductoVarianteResponse;
import com.easybeach.concierge.web.dto.TipoServicioRequest;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.payments.web.dto.EstadoVinculacionResponse;
import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Etapa 19 §2: batería cross-tenant sistemática sobre los ABM de admin que
 * {@link com.easybeach.catalog.CatalogoCrossTenantIntegrationTest} no cubre
 * (esa clase solo prueba categorías y ubicaciones): branding, productos,
 * variantes de producto, tipos de servicio, vinculación de Mercado Pago, y
 * los endpoints "propio balneario" (admin/balneario, staff/balneario) que no
 * toman ningún id de path y por eso necesitan una prueba de aislamiento
 * positiva en vez de un intento de acceso a un id ajeno.
 */
class CrossTenantAdminAbmIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto a;
    private EscenarioBalneario.Contexto b;

    @BeforeEach
    void seedDosBalnearios() {
        a = escenario.crearBalnearioOperativoConStaff("abm-a");
        b = escenario.crearBalnearioOperativoConStaff("abm-b");
    }

    // ---------------------------------------------------------------- productos

    @Test
    void adminDeBNoPuedeEditarNiBorrarProductoDeA() {
        Long categoriaA = crearCategoria(a, "Bebidas A");
        Long productoA = crearProducto(a, categoriaA, "Cerveza A");

        var editar = restTemplate.exchange(url("/api/v1/admin/productos/" + productoA), HttpMethod.PUT,
                new HttpEntity<>(new ProductoRequest(categoriaA, "Hackeado", null, new BigDecimal("1.00"), true, 1),
                        b.adminHeaders()),
                ProblemDetail.class);
        assertThat(editar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var disponibilidad = restTemplate.exchange(url("/api/v1/admin/productos/" + productoA + "/disponibilidad"),
                HttpMethod.PUT, new HttpEntity<>(new DisponibilidadRequest(false), b.adminHeaders()),
                ProblemDetail.class);
        assertThat(disponibilidad.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var borrar = restTemplate.exchange(url("/api/v1/admin/productos/" + productoA), HttpMethod.DELETE,
                new HttpEntity<>(b.adminHeaders()), ProblemDetail.class);
        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // El producto de A sigue intacto.
        ProductoResponse[] listadoDeA = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), ProductoResponse[].class).getBody();
        assertThat(listadoDeA).extracting(ProductoResponse::nombre).containsExactly("Cerveza A");
    }

    @Test
    void adminDeBNoVeProductosDeAEnSuListado() {
        Long categoriaA = crearCategoria(a, "Bebidas A2");
        crearProducto(a, categoriaA, "Cerveza A2");

        ProductoResponse[] listadoDeB = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.GET,
                new HttpEntity<>(b.adminHeaders()), ProductoResponse[].class).getBody();
        assertThat(listadoDeB).isEmpty();
    }

    // ---------------------------------------------------------------- variantes

    @Test
    void adminDeBNoPuedeEditarNiBorrarVarianteDeA() {
        Long categoriaA = crearCategoria(a, "Bebidas A3");
        Long productoA = crearProducto(a, categoriaA, "Cerveza A3");
        ProductoVarianteResponse variante = restTemplate.exchange(
                url("/api/v1/admin/productos/" + productoA + "/variantes"), HttpMethod.POST,
                new HttpEntity<>(new ProductoVarianteRequest("Litro", new BigDecimal("3000.00"), true, 1),
                        a.adminHeaders()),
                ProductoVarianteResponse.class).getBody();

        // Ni siquiera referenciando el productoId real de A: el service valida
        // ambos ids (productoId Y varianteId) contra el balnearioId del actor.
        var editar = restTemplate.exchange(
                url("/api/v1/admin/productos/" + productoA + "/variantes/" + variante.id()), HttpMethod.PUT,
                new HttpEntity<>(new ProductoVarianteRequest("Hackeada", new BigDecimal("1.00"), true, 1),
                        b.adminHeaders()),
                ProblemDetail.class);
        assertThat(editar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var borrar = restTemplate.exchange(
                url("/api/v1/admin/productos/" + productoA + "/variantes/" + variante.id()), HttpMethod.DELETE,
                new HttpEntity<>(b.adminHeaders()), ProblemDetail.class);
        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- tipos de servicio

    @Test
    void adminDeBNoPuedeEditarNiBorrarTipoDeServicioDeA() {
        TipoServicioResponse tipoA = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.POST,
                new HttpEntity<>(new TipoServicioRequest("Sombrilla extra", true, 1), a.adminHeaders()),
                TipoServicioResponse.class).getBody();

        var editar = restTemplate.exchange(url("/api/v1/admin/tipos-servicio/" + tipoA.id()), HttpMethod.PUT,
                new HttpEntity<>(new TipoServicioRequest("Hackeado", true, 1), b.adminHeaders()),
                ProblemDetail.class);
        assertThat(editar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var borrar = restTemplate.exchange(url("/api/v1/admin/tipos-servicio/" + tipoA.id()), HttpMethod.DELETE,
                new HttpEntity<>(b.adminHeaders()), ProblemDetail.class);
        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        TipoServicioResponse[] listadoDeA = restTemplate.exchange(url("/api/v1/admin/tipos-servicio"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), TipoServicioResponse[].class).getBody();
        assertThat(listadoDeA).extracting(TipoServicioResponse::nombre).containsExactly("Sombrilla extra");
    }

    // ---------------------------------------------------------------- branding (sin id de path: aislamiento por JWT)

    @Test
    void brandingDeCadaBalnearioEsIndependiente() {
        // Mismos colores (la paleta neutra por defecto, que sabemos que cumple
        // contraste) para ambos - lo que varía y lo que prueba el aislamiento
        // es el nombre del theme, no los colores en sí.
        var putA = restTemplate.exchange(url("/api/v1/admin/branding"), HttpMethod.PUT,
                new HttpEntity<>(brandingRequest("Theme de A"), a.adminHeaders()), BrandingUpdateResult.class);
        var putB = restTemplate.exchange(url("/api/v1/admin/branding"), HttpMethod.PUT,
                new HttpEntity<>(brandingRequest("Theme de B"), b.adminHeaders()), BrandingUpdateResult.class);
        assertThat(putA.getBody().aplicado()).isTrue();
        assertThat(putB.getBody().aplicado()).isTrue();

        var brandingDeA = restTemplate.exchange(url("/api/v1/admin/branding"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), java.util.Map.class).getBody();
        var brandingDeB = restTemplate.exchange(url("/api/v1/admin/branding"), HttpMethod.GET,
                new HttpEntity<>(b.adminHeaders()), java.util.Map.class).getBody();

        assertThat(brandingDeA.get("theme.name")).isEqualTo("Theme de A");
        assertThat(brandingDeB.get("theme.name")).isEqualTo("Theme de B");
    }

    private BrandingUpdateRequest brandingRequest(String nombre) {
        return new BrandingUpdateRequest(nombre, "#C95100", "#17437B", "#F5EFE2", "#FFFFFF",
                "#1E7D3C", "#B25E00", "#C22F2F", "#1D62B4", TypographyFamily.CLARA, true);
    }

    // ---------------------------------------------------------------- mercadopago (sin id de path)

    @Test
    void estadoDeVinculacionMpEsIndependientePorBalneario() {
        // EscenarioBalneario vincula MP a ambos con un mpUserId derivado del balnearioId propio.
        var estadoA = restTemplate.exchange(url("/api/v1/admin/mercadopago/estado"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), EstadoVinculacionResponse.class).getBody();
        var estadoB = restTemplate.exchange(url("/api/v1/admin/mercadopago/estado"), HttpMethod.GET,
                new HttpEntity<>(b.adminHeaders()), EstadoVinculacionResponse.class).getBody();

        assertThat(estadoA.vinculado()).isTrue();
        assertThat(estadoB.vinculado()).isTrue();
        assertThat(estadoA.mpUserId()).isNotEqualTo(estadoB.mpUserId());
        assertThat(estadoA.mpUserId()).contains(a.balnearioId().toString());
        assertThat(estadoB.mpUserId()).contains(b.balnearioId().toString());
    }

    @Test
    void desvincularMpDeBNoAfectaAA() {
        restTemplate.exchange(url("/api/v1/admin/mercadopago/desvincular"), HttpMethod.POST,
                new HttpEntity<>(b.adminHeaders()), Void.class);

        var estadoA = restTemplate.exchange(url("/api/v1/admin/mercadopago/estado"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), EstadoVinculacionResponse.class).getBody();
        assertThat(estadoA.vinculado()).isTrue();
    }

    // ---------------------------------------------------------------- balneario propio (admin/balneario, staff/balneario)

    @Test
    void adminBalnearioYStaffBalnearioDevuelvenSoloElPropio() {
        var adminVeA = restTemplate.exchange(url("/api/v1/admin/balneario"), HttpMethod.GET,
                new HttpEntity<>(a.adminHeaders()), BalnearioResponse.class).getBody();
        var adminVeB = restTemplate.exchange(url("/api/v1/admin/balneario"), HttpMethod.GET,
                new HttpEntity<>(b.adminHeaders()), BalnearioResponse.class).getBody();
        assertThat(adminVeA.id()).isEqualTo(a.balnearioId());
        assertThat(adminVeB.id()).isEqualTo(b.balnearioId());
        assertThat(adminVeA.id()).isNotEqualTo(adminVeB.id());

        var carperoVeA = restTemplate.exchange(url("/api/v1/staff/balneario"), HttpMethod.GET,
                new HttpEntity<>(a.carperoHeaders()), BalnearioResponse.class).getBody();
        assertThat(carperoVeA.id()).isEqualTo(a.balnearioId());
    }

    // ---------------------------------------------------------------- helpers

    private Long crearCategoria(EscenarioBalneario.Contexto ctx, String nombre) {
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest(nombre, 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        return categoria.id();
    }

    private Long crearProducto(EscenarioBalneario.Contexto ctx, Long categoriaId, String nombre) {
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoriaId, nombre, null, new BigDecimal("2000.00"), true, 1),
                        ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        return producto.id();
    }
}
