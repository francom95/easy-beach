package com.easybeach.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
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
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Etapa 11 criterio de aceptación: "un admin del balneario A no puede tocar
 * catálogo ni ubicaciones del B". Prueba explícita, no incidental.
 */
class CatalogoCrossTenantIntegrationTest extends AbstractIntegrationTest {

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

    private HttpHeaders adminAHeaders;
    private HttpHeaders adminBHeaders;
    private Long categoriaDeA;
    private Long ubicacionDeA;

    @BeforeEach
    void seedDosBalneariosConSusAdmins() {
        adminAHeaders = crearBalnearioYObtenerHeadersDeAdmin("balneario-a");
        adminBHeaders = crearBalnearioYObtenerHeadersDeAdmin("balneario-b");

        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Categoría de A", 1, true), adminAHeaders),
                CategoriaMenuResponse.class).getBody();
        categoriaDeA = categoria.id();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa de A"), adminAHeaders),
                UbicacionResponse.class).getBody();
        ubicacionDeA = ubicacion.id();
    }

    @Test
    void adminDeBNoPuedeEditarNiBorrarCategoriaDeA() {
        var editar = restTemplate.exchange(url("/api/v1/admin/categorias/" + categoriaDeA), HttpMethod.PUT,
                new HttpEntity<>(new CategoriaMenuRequest("Hackeada", 1, true), adminBHeaders), ProblemDetail.class);
        assertThat(editar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var borrar = restTemplate.exchange(url("/api/v1/admin/categorias/" + categoriaDeA), HttpMethod.DELETE,
                new HttpEntity<>(adminBHeaders), ProblemDetail.class);
        assertThat(borrar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // La categoría de A sigue intacta, sin importar el intento de B.
        var listadoDeA = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.GET,
                new HttpEntity<>(adminAHeaders), CategoriaMenuResponse[].class).getBody();
        assertThat(listadoDeA).extracting(CategoriaMenuResponse::nombre).containsExactly("Categoría de A");
    }

    @Test
    void adminDeBNoPuedeEditarUbicacionDeA() {
        var editar = restTemplate.exchange(url("/api/v1/admin/ubicaciones/" + ubicacionDeA), HttpMethod.PUT,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.MESA, "Hackeada"), adminBHeaders),
                ProblemDetail.class);
        assertThat(editar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listadosDeBNuncaIncluyenDatosDeA() {
        UbicacionResponse[] ubicacionesDeB = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.GET,
                new HttpEntity<>(adminBHeaders), UbicacionResponse[].class).getBody();
        assertThat(ubicacionesDeB).isEmpty();

        CategoriaMenuResponse[] categoriasDeB = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.GET,
                new HttpEntity<>(adminBHeaders), CategoriaMenuResponse[].class).getBody();
        assertThat(categoriasDeB).isEmpty();
    }

    private HttpHeaders crearBalnearioYObtenerHeadersDeAdmin(String slugPrefix) {
        Balneario balneario = new Balneario();
        balneario.setSlug(slugPrefix + "-" + System.nanoTime());
        balneario.setNombre(slugPrefix);
        balneario.setEmailContacto(slugPrefix + "@example.com");
        balneario = balnearioRepository.save(balneario);

        Usuario admin = new Usuario();
        String email = slugPrefix + "." + System.nanoTime() + "@example.com";
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode("ClaveSegura123"));
        admin.setNombre("Admin " + slugPrefix);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        return headers;
    }
}
