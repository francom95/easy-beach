package com.easybeach.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Foto de producto (etapa 17): {@code ProductoRequest} nunca tuvo forma de
 * subirla - gap real encontrado al diseñar el panel admin (pantalla "Menú").
 * Mismo storage (magic bytes, fuera del web root) que branding, movido a
 * {@code shared} porque {@code catalog} no puede depender de {@code branding}
 * (ADR-002).
 */
class ProductoFotoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private Long productoId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("foto-producto");
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Cerveza", "Bien fría",
                        new BigDecimal("2000.00"), true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();
    }

    @Test
    void subeFotoValidaYQuedaAccesiblePorUrlPublica() {
        var response = subirFoto(pngMinimo(), "foto.png", ctx.adminHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String fotoUrl = response.getBody().fotoUrl();
        assertThat(fotoUrl).startsWith("/public/assets/balnearios/" + ctx.balnearioId() + "/productos/");

        var fetched = restTemplate.getForEntity(url(fotoUrl), byte[].class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isEqualTo(pngMinimo());
    }

    @Test
    void archivoQueNoEsUnaImagenRealEsRechazado() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("no es una imagen".getBytes()) {
            @Override
            public String getFilename() {
                return "foto.png";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(ctx.adminHeaders());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = restTemplate.exchange(url("/api/v1/admin/productos/" + productoId + "/foto"), HttpMethod.POST,
                new HttpEntity<>(body, headers), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void noSePuedeSubirFotoAUnProductoDeOtroBalneario() {
        EscenarioBalneario.Contexto otro = escenario.crearBalnearioOperativoConStaff("foto-otro-balneario");

        var response = subirFoto(pngMinimo(), "foto.png", otro.adminHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private org.springframework.http.ResponseEntity<ProductoResponse> subirFoto(byte[] bytes, String filename, HttpHeaders authHeaders) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(authHeaders);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return restTemplate.exchange(url("/api/v1/admin/productos/" + productoId + "/foto"), HttpMethod.POST,
                new HttpEntity<>(body, headers), ProductoResponse.class);
    }

    /** PNG 1x1 válido (magic bytes + IHDR mínimo) para tests. */
    private byte[] pngMinimo() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                0x42, 0x60, (byte) 0x82
        };
    }
}
