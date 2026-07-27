package com.easybeach.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.identity.web.dto.InvitarStaffRequest;
import com.easybeach.identity.web.dto.InvitarStaffResponse;
import com.easybeach.identity.web.dto.MiembroResponse;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * ABM real de staff (etapa 17): {@code /staff/miembros} (etapa 09) era solo
 * de lectura - gap real encontrado al diseñar la pantalla "Staff" del panel
 * admin (mockup de etapa 08).
 */
class AdminStaffIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    @Test
    void invitaListaYRevocaUnCarpero() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("staff-abm");
        String email = "nuevo.carpero." + System.nanoTime() + "@example.com";

        var invitacion = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.POST,
                new HttpEntity<>(new InvitarStaffRequest(email, "Nuevo Carpero", RolCodigo.CARPERO), ctx.adminHeaders()),
                InvitarStaffResponse.class);
        assertThat(invitacion.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(invitacion.getBody().passwordTemporal()).isNotBlank();
        assertThat(invitacion.getBody().rol()).isEqualTo("CARPERO");

        MiembroResponse[] miembros = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), MiembroResponse[].class).getBody();
        assertThat(miembros).extracting(MiembroResponse::usuarioEmail).contains(email);

        String usuarioPublicId = invitacion.getBody().usuarioPublicId();
        var revocacion = restTemplate.exchange(url("/api/v1/admin/staff/" + usuarioPublicId), HttpMethod.DELETE,
                new HttpEntity<>(ctx.adminHeaders()), Void.class);
        assertThat(revocacion.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        MiembroResponse[] despuesDeRevocar = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), MiembroResponse[].class).getBody();
        assertThat(despuesDeRevocar).extracting(MiembroResponse::usuarioEmail).doesNotContain(email);
    }

    @Test
    void noSePuedeInvitarOtroAdminBalneario() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("staff-rol-invalido");

        var response = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.POST,
                new HttpEntity<>(new InvitarStaffRequest("otro.admin@example.com", "Otro Admin", RolCodigo.ADMIN_BALNEARIO),
                        ctx.adminHeaders()),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.valueOf(422));
    }

    @Test
    void noSePuedeInvitarUnEmailYaRegistrado() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("staff-email-duplicado");
        String email = "duplicado." + System.nanoTime() + "@example.com";
        restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.POST,
                new HttpEntity<>(new InvitarStaffRequest(email, "Primero", RolCodigo.CARPERO), ctx.adminHeaders()),
                InvitarStaffResponse.class);

        var response = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.POST,
                new HttpEntity<>(new InvitarStaffRequest(email, "Segundo", RolCodigo.OPERADOR), ctx.adminHeaders()),
                ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void noSePuedeRevocarAlUnicoAdmin() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("staff-unico-admin");
        MiembroResponse[] miembros = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.GET,
                new HttpEntity<>(ctx.adminHeaders()), MiembroResponse[].class).getBody();
        String adminPublicId = java.util.Arrays.stream(miembros)
                .filter(m -> m.rol().equals("ADMIN_BALNEARIO"))
                .findFirst().orElseThrow().usuarioPublicId();

        var response = restTemplate.exchange(url("/api/v1/admin/staff/" + adminPublicId), HttpMethod.DELETE,
                new HttpEntity<>(ctx.adminHeaders()), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void operadorNoPuedeInvitarStaff() {
        EscenarioBalneario.Contexto ctx = escenario.crearBalnearioOperativoConStaff("staff-sin-permiso");

        var response = restTemplate.exchange(url("/api/v1/admin/staff"), HttpMethod.POST,
                new HttpEntity<>(new InvitarStaffRequest("x@example.com", "X", RolCodigo.CARPERO), ctx.operadorHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
