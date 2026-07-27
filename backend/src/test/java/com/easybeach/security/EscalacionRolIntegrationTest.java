package com.easybeach.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Etapa 19 §2: matriz de escalación de rol. Antes de esta clase, cada 403 de
 * rol vivía disperso e incidental en el archivo del módulo que lo motivó
 * (identity, platform, reporting), sin un lugar único que pruebe la matriz
 * rol×endpoint completa. En particular, ningún test existente probaba
 * CARPERO contra un endpoint solo-OPERADOR (o viceversa) - los dos roles
 * operativos con permisos distintos pero superpuestos (ambos heredados por
 * ADMIN_BALNEARIO, etapa 05 §2) - ni CARPERO/OPERADOR contra Super Admin.
 */
class EscalacionRolIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("escalacion");
    }

    // ---------------------------------------------------------------- CARPERO / OPERADOR entre sí

    @Test
    void carperoNoAccedeAColaDePedidosSoloDeOperador() {
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.carperoHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorNoAccedeAColaDeEstadiasPendientesSoloDeCarpero() {
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorNoAccedeAColaDeSolicitudesServicioSoloDeCarpero() {
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/solicitudes-servicio/cola"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------- CARPERO / OPERADOR contra endpoints solo-ADMIN

    @Test
    void carperoNoPuedeCrearProductos() {
        var respuesta = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(1L, "x", null, new BigDecimal("1.00"), true, 1),
                        ctx.carperoHeaders()),
                ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorNoPuedeCrearUbicaciones() {
        var respuesta = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "x"), ctx.operadorHeaders()),
                ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void carperoNoPuedeVerReportes() {
        var respuesta = restTemplate.exchange(url("/api/v1/admin/reportes/dashboard"), HttpMethod.GET,
                new HttpEntity<>(ctx.carperoHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------- CLIENTE contra endpoints de staff

    @Test
    void clienteNoAccedeAColaDePedidosOperativa() {
        HttpHeaders cliente = escenario.registrarClienteYObtenerHeaders();
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(cliente), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clienteNoAccedeAColaDeEstadiasPendientes() {
        HttpHeaders cliente = escenario.registrarClienteYObtenerHeaders();
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/estadias/pendientes"), HttpMethod.GET,
                new HttpEntity<>(cliente), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clienteNoPuedeCrearProductosDeAdmin() {
        HttpHeaders cliente = escenario.registrarClienteYObtenerHeaders();
        var respuesta = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(1L, "x", null, new BigDecimal("1.00"), true, 1), cliente),
                ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------- staff (cualquier rol) contra Super Admin

    @Test
    void adminBalnearioNoPuedeCrearPlanes() {
        var respuesta = restTemplate.exchange(url("/api/v1/super-admin/planes"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminBalnearioNoPuedeVerTemporadas() {
        var respuesta = restTemplate.exchange(url("/api/v1/super-admin/temporadas"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminBalnearioNoPuedeVerAuditoria() {
        var respuesta = restTemplate.exchange(url("/api/v1/super-admin/auditoria"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void carperoNoPuedeVerBalneariosDeSuperAdmin() {
        var respuesta = restTemplate.exchange(url("/api/v1/super-admin/balnearios"), HttpMethod.GET,
                new HttpEntity<>(ctx.carperoHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void operadorNoPuedeVerAuditoria() {
        var respuesta = restTemplate.exchange(url("/api/v1/super-admin/auditoria"), HttpMethod.GET,
                new HttpEntity<>(ctx.operadorHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---------------------------------------------------------------- sin token

    @Test
    void sinTokenColaDePedidosEsRechazadaConNoAutorizado() {
        var respuesta = restTemplate.exchange(url("/api/v1/operativo/pedidos/cola"), HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), ProblemDetail.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
