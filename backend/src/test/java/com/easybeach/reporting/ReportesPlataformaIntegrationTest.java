package com.easybeach.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.reporting.dto.PlataformaReporteResponse;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
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
import org.springframework.http.ProblemDetail;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Etapa 15 criterio de aceptación: el reporte de plataforma es el ÚNICO que
 * cruza datos entre balnearios, y solo Super Admin puede verlo.
 */
class ReportesPlataformaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders superAdminHeaders;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("plataforma");
        superAdminHeaders = crearSuperAdminYObtenerHeaders();

        HttpHeaders clienteHeaders = escenario.registrarClienteYObtenerHeaders();
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.MESA, "Mesa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Comidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Papas", null, new BigDecimal("500.00"),
                        true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);

        HttpHeaders pedidoHeaders = new HttpHeaders();
        pedidoHeaders.addAll(clienteHeaders);
        pedidoHeaders.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadia.publicId(),
                List.of(new CrearPedidoRequest.ItemRequest(producto.id(), null, 1)), "fake-card-token");
        String pedidoPublicId = restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST,
                new HttpEntity<>(request, pedidoHeaders), PedidoResponse.class).getBody().publicId();
        for (EstadoPedido destino : List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO)) {
            restTemplate.exchange(url("/api/v1/operativo/pedidos/" + pedidoPublicId + "/estado"), HttpMethod.PUT,
                    new HttpEntity<>(new TransicionPedidoRequest(destino, null), ctx.operadorHeaders()),
                    PedidoResponse.class);
        }
    }

    private HttpHeaders crearSuperAdminYObtenerHeaders() {
        Usuario superAdmin = new Usuario();
        String email = "super." + System.nanoTime() + "@example.com";
        superAdmin.setEmail(email);
        superAdmin.setPasswordHash(passwordEncoder.encode("ClaveSegura123"));
        superAdmin.setNombre("Super Admin");
        superAdmin.setTipo(TipoUsuario.SUPER_ADMIN);
        superAdmin.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(superAdmin);

        TokenResponse tokens = restTemplate.postForEntity("/api/v1/auth/login/super-admin",
                new LoginRequest(email, "ClaveSegura123"), TokenResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        return headers;
    }

    @Test
    void elReporteDePlataformaIncluyeElVolumenDeMiBalneario() {
        PlataformaReporteResponse reporte = restTemplate.exchange(url("/api/v1/super-admin/reportes/plataforma"),
                HttpMethod.GET, new HttpEntity<>(superAdminHeaders), PlataformaReporteResponse.class).getBody();

        assertThat(reporte.balneariosActivos()).isGreaterThanOrEqualTo(1);
        assertThat(reporte.volumenPorBalneario())
                .filteredOn(v -> v.balnearioId().equals(ctx.balnearioId()))
                .singleElement()
                .satisfies(v -> {
                    assertThat(v.cantidadPedidos()).isEqualTo(1);
                    assertThat(v.facturacion()).isEqualByComparingTo("500.00");
                });
    }

    @Test
    void unAdminDeBalnearioNoPuedeVerElReporteDePlataforma() {
        var response = restTemplate.exchange(url("/api/v1/super-admin/reportes/plataforma"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
