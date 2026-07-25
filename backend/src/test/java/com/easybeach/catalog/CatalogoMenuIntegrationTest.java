package com.easybeach.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.DisponibilidadRequest;
import com.easybeach.catalog.web.dto.MenuCategoriaResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.catalog.web.dto.ProductoVarianteRequest;
import com.easybeach.catalog.web.dto.ProductoVarianteResponse;
import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Etapa 11: ABM de categorías/productos/variantes y el menú público (el
 * endpoint más consultado de la plataforma).
 */
class CatalogoMenuIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BalnearioRepository balnearioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String slug;
    private HttpHeaders adminHeaders;

    @BeforeEach
    void seedBalnearioConAdmin() {
        Balneario balneario = new Balneario();
        slug = "balneario-menu-" + System.nanoTime();
        balneario.setSlug(slug);
        balneario.setNombre("Balneario Menú");
        balneario.setEmailContacto("contacto@balneario.com");
        balneario = balnearioRepository.save(balneario);

        Usuario admin = new Usuario();
        String email = "admin." + System.nanoTime() + "@balneario.com";
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("ClaveSegura123"));
        admin.setNombre("Admin");
        admin.setTipo(TipoUsuario.STAFF);
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin = usuarioRepository.save(admin);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(admin);
        vinculo.setBalnearioId(balneario.getId());
        vinculo.setRol(rolRepository.findByCodigo(RolCodigo.ADMIN_BALNEARIO).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);

        TokenResponse tokens = restTemplate.postForEntity(url("/api/v1/auth/login/staff"),
                new LoginRequest(email, "ClaveSegura123"), TokenResponse.class).getBody();
        adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(tokens.accessToken());
    }

    @Test
    void flujoCompletoDeMenuYDisponibilidad() {
        // 1. Crear categoría.
        var categoriaRequest = new CategoriaMenuRequest("Bebidas", 1, true);
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(categoriaRequest, adminHeaders), CategoriaMenuResponse.class).getBody();

        // 2. Crear producto con variantes.
        var productoRequest = new ProductoRequest(categoria.id(), "Gaseosa", "Bien fría", new BigDecimal("2000.00"), true, 1);
        ResponseEntity<ProductoResponse> productoResponse = restTemplate.exchange(url("/api/v1/admin/productos"),
                HttpMethod.POST, new HttpEntity<>(productoRequest, adminHeaders), ProductoResponse.class);
        assertThat(productoResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductoResponse producto = productoResponse.getBody();

        var varianteChica = new ProductoVarianteRequest("Chica (350ml)", new BigDecimal("1800.00"), true, 1);
        var varianteGrande = new ProductoVarianteRequest("Grande (500ml)", new BigDecimal("2200.00"), true, 2);
        restTemplate.exchange(url("/api/v1/admin/productos/" + producto.id() + "/variantes"), HttpMethod.POST,
                new HttpEntity<>(varianteChica, adminHeaders), ProductoVarianteResponse.class);
        restTemplate.exchange(url("/api/v1/admin/productos/" + producto.id() + "/variantes"), HttpMethod.POST,
                new HttpEntity<>(varianteGrande, adminHeaders), ProductoVarianteResponse.class);

        // 3. El menú público lo devuelve todo en una sola llamada.
        MenuCategoriaResponse[] menu = restTemplate.getForObject(url("/api/v1/balnearios/" + slug + "/menu"),
                MenuCategoriaResponse[].class);
        assertThat(menu).hasSize(1);
        assertThat(menu[0].productos()).hasSize(1);
        assertThat(menu[0].productos().get(0).variantes()).hasSize(2);
        assertThat(menu[0].productos().get(0).variantes())
                .extracting(v -> v.precio().compareTo(new BigDecimal("1800.00")) == 0 || v.precio().compareTo(new BigDecimal("2200.00")) == 0)
                .allMatch(Boolean::booleanValue);

        // 4. Apagar disponibilidad: desaparece del menú público al instante.
        restTemplate.exchange(url("/api/v1/admin/productos/" + producto.id() + "/disponibilidad"), HttpMethod.PUT,
                new HttpEntity<>(new DisponibilidadRequest(false), adminHeaders), ProductoResponse.class);

        MenuCategoriaResponse[] menuTrasApagar = restTemplate.getForObject(
                url("/api/v1/balnearios/" + slug + "/menu"), MenuCategoriaResponse[].class);
        assertThat(menuTrasApagar[0].productos()).isEmpty();

        // 5. No se puede borrar la categoría mientras tenga productos.
        var eliminarCategoria = restTemplate.exchange(url("/api/v1/admin/categorias/" + categoria.id()),
                HttpMethod.DELETE, new HttpEntity<>(adminHeaders), ProblemDetail.class);
        assertThat(eliminarCategoria.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // 6. Borrar el producto y ENTONCES sí se puede borrar la categoría.
        restTemplate.exchange(url("/api/v1/admin/productos/" + producto.id()), HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), Void.class);
        var eliminarCategoriaOk = restTemplate.exchange(url("/api/v1/admin/categorias/" + categoria.id()),
                HttpMethod.DELETE, new HttpEntity<>(adminHeaders), Void.class);
        assertThat(eliminarCategoriaOk.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        CategoriaMenuResponse[] categoriasRestantes = restTemplate.exchange(url("/api/v1/admin/categorias"),
                HttpMethod.GET, new HttpEntity<>(adminHeaders), CategoriaMenuResponse[].class).getBody();
        assertThat(categoriasRestantes).isEmpty();
    }

    @Test
    void ubicacionDuplicadaEsRechazada() {
        var request = new com.easybeach.stay.web.dto.UbicacionRequest(
                com.easybeach.stay.domain.TipoUbicacion.CARPA, "Carpa 12");
        var primera = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(request, adminHeaders), com.easybeach.stay.web.dto.UbicacionResponse.class);
        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var duplicada = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(request, adminHeaders), ProblemDetail.class);
        assertThat(duplicada.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
