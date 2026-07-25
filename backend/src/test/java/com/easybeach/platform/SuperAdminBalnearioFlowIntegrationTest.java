package com.easybeach.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.service.BalnearioMpCredencialService;
import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.web.dto.ActualizarBalnearioRequest;
import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.platform.web.dto.CambiarEstadoSuscripcionRequest;
import com.easybeach.platform.web.dto.CrearBalnearioRequest;
import com.easybeach.platform.web.dto.CrearBalnearioResponse;
import com.easybeach.platform.web.dto.MotivoRequest;
import com.easybeach.platform.web.dto.PlanRequest;
import com.easybeach.platform.web.dto.PlanResponse;
import com.easybeach.platform.web.dto.SuscribirRequest;
import com.easybeach.platform.web.dto.SuscripcionResponse;
import com.easybeach.platform.web.dto.TemporadaRequest;
import com.easybeach.platform.web.dto.TemporadaResponse;
import com.easybeach.support.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
 * Flujo completo de la etapa 10 (criterio de aceptación): crear balneario →
 * vincular Mercado Pago (OAuth) → configurar branding → suscribir a
 * temporada → aparece en el listado público → suspender → desaparece y
 * rechaza operaciones.
 */
class SuperAdminBalnearioFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private BalnearioMpCredencialService balnearioMpCredencialService;

    private String superAdminEmail;
    private HttpHeaders superAdminHeaders;

    @BeforeEach
    void seedSuperAdmin() {
        Usuario superAdmin = new Usuario();
        superAdminEmail = "superadmin." + System.nanoTime() + "@easybeach.dev";
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setPasswordHash(passwordEncoder.encode("ClaveSuperSegura123"));
        superAdmin.setNombre("Super Admin de Prueba");
        superAdmin.setTipo(TipoUsuario.SUPER_ADMIN);
        superAdmin.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(superAdmin);

        TokenResponse tokens = restTemplate.postForEntity(url("/api/v1/auth/login/super-admin"),
                new LoginRequest(superAdminEmail, "ClaveSuperSegura123"), TokenResponse.class).getBody();
        superAdminHeaders = new HttpHeaders();
        superAdminHeaders.setBearerAuth(tokens.accessToken());
    }

    @Test
    void flujoCompletoDeAltaDeBalneario() {
        String slug = "costa-nena-" + System.nanoTime();

        // 1. Crear balneario (crea también su primer admin con password temporal).
        var crearRequest = new CrearBalnearioRequest(slug, "Costa Nena", "contacto@costanena.com.ar", "+54 223 555-0000",
                "Admin Costa Nena", "admin." + System.nanoTime() + "@costanena.com.ar");
        ResponseEntity<CrearBalnearioResponse> crearResponse = restTemplate.exchange(
                url("/api/v1/super-admin/balnearios"), HttpMethod.POST,
                new HttpEntity<>(crearRequest, superAdminHeaders), CrearBalnearioResponse.class);
        assertThat(crearResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long balnearioId = crearResponse.getBody().balneario().id();
        assertThat(crearResponse.getBody().passwordTemporalAdmin()).isNotBlank();
        assertThat(crearResponse.getBody().balneario().operativo()).isFalse(); // todavía sin suscripción

        // 2. Vincular Mercado Pago (fake en tests - ver FakeMercadoPagoOAuthClient).
        String adminEmail = crearRequest.emailAdmin();
        TokenResponse adminTokensIniciales = restTemplate.postForEntity(url("/api/v1/auth/login/staff"),
                new LoginRequest(adminEmail, crearResponse.getBody().passwordTemporalAdmin()), TokenResponse.class).getBody();
        assertThat(adminTokensIniciales.debeCambiarPassword()).isTrue();
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminTokensIniciales.accessToken());

        assertThat(balnearioMpCredencialService.puedeRecibirPagos(balnearioId)).isFalse();

        var iniciarResponse = restTemplate.exchange(url("/api/v1/admin/mercadopago/oauth/iniciar"),
                HttpMethod.GET, new HttpEntity<>(adminHeaders), com.easybeach.payments.web.dto.IniciarVinculacionResponse.class);
        assertThat(iniciarResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String state = extractQueryParam(iniciarResponse.getBody().urlAutorizacion(), "state");

        var callbackResponse = restTemplate.getForEntity(
                url("/api/v1/mercadopago/oauth/callback?state=" + state + "&code=fake-auth-code"),
                com.easybeach.payments.web.dto.CallbackResultResponse.class);
        assertThat(callbackResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(callbackResponse.getBody().vinculado()).isTrue();
        assertThat(balnearioMpCredencialService.puedeRecibirPagos(balnearioId)).isTrue();

        var estadoMp = restTemplate.exchange(url("/api/v1/admin/mercadopago/estado"), HttpMethod.GET,
                new HttpEntity<>(adminHeaders), com.easybeach.payments.web.dto.EstadoVinculacionResponse.class).getBody();
        assertThat(estadoMp.vinculado()).isTrue();
        assertThat(estadoMp.estado()).isEqualTo(EstadoCredencialMp.VINCULADA.name());

        // 3. Configurar branding (ya viene con el default sembrado al crear el balneario).
        var brandingActual = restTemplate.exchange(url("/api/v1/admin/branding"), HttpMethod.GET,
                new HttpEntity<>(adminHeaders), java.util.Map.class).getBody();
        assertThat(brandingActual).containsKey("color.primary");

        // 4. Suscribir a plan + temporada.
        var planRequest = new PlanRequest("Plan Estándar", "Plan de prueba", new BigDecimal("15000.00"), true);
        PlanResponse plan = restTemplate.exchange(url("/api/v1/super-admin/planes"), HttpMethod.POST,
                new HttpEntity<>(planRequest, superAdminHeaders), PlanResponse.class).getBody();

        var temporadaRequest = new TemporadaRequest("Verano " + System.nanoTime(),
                LocalDate.now().minusDays(1), LocalDate.now().plusMonths(3));
        TemporadaResponse temporada = restTemplate.exchange(url("/api/v1/super-admin/temporadas"), HttpMethod.POST,
                new HttpEntity<>(temporadaRequest, superAdminHeaders), TemporadaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/super-admin/temporadas/" + temporada.id() + "/estado"), HttpMethod.PUT,
                new HttpEntity<>(new com.easybeach.platform.web.dto.CambiarEstadoTemporadaRequest(EstadoTemporada.EN_CURSO),
                        superAdminHeaders),
                TemporadaResponse.class);

        var suscribirRequest = new SuscribirRequest(plan.id(), temporada.id());
        ResponseEntity<SuscripcionResponse> suscripcionResponse = restTemplate.exchange(
                url("/api/v1/super-admin/balnearios/" + balnearioId + "/suscripciones"), HttpMethod.POST,
                new HttpEntity<>(suscribirRequest, superAdminHeaders), SuscripcionResponse.class);
        assertThat(suscripcionResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(suscripcionResponse.getBody().estado()).isEqualTo(EstadoSuscripcion.ACTIVA.name());
        Long suscripcionId = suscripcionResponse.getBody().id();

        // 5. Aparece en el listado público.
        List<BalnearioResponse> listadoPublico = List.of(restTemplate.getForObject(
                url("/api/v1/balnearios"), BalnearioResponse[].class));
        assertThat(listadoPublico).anyMatch(b -> b.id().equals(balnearioId) && b.operativo());

        BalnearioResponse detallePublico = restTemplate.getForObject(url("/api/v1/balnearios/" + slug), BalnearioResponse.class);
        assertThat(detallePublico.operativo()).isTrue();

        // 6. Suspender: desaparece del listado público y rechaza operaciones.
        restTemplate.exchange(url("/api/v1/super-admin/balnearios/" + balnearioId + "/suspender"), HttpMethod.POST,
                new HttpEntity<>(new MotivoRequest("Pago de plataforma vencido"), superAdminHeaders), BalnearioResponse.class);

        List<BalnearioResponse> listadoTrasSuspender = List.of(restTemplate.getForObject(
                url("/api/v1/balnearios"), BalnearioResponse[].class));
        assertThat(listadoTrasSuspender).noneMatch(b -> b.id().equals(balnearioId));

        ResponseEntity<ProblemDetail> detalleTrasSuspender = restTemplate.getForEntity(
                url("/api/v1/balnearios/" + slug), ProblemDetail.class);
        assertThat(detalleTrasSuspender.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // Reactivar + suspender la suscripción (en vez del balneario) también lo saca de operativo.
        restTemplate.exchange(url("/api/v1/super-admin/balnearios/" + balnearioId + "/activar"), HttpMethod.POST,
                new HttpEntity<>(new MotivoRequest("Pago regularizado"), superAdminHeaders), BalnearioResponse.class);
        restTemplate.exchange(url("/api/v1/super-admin/balnearios/" + balnearioId + "/suscripciones/" + suscripcionId + "/estado"),
                HttpMethod.PUT,
                new HttpEntity<>(new CambiarEstadoSuscripcionRequest(EstadoSuscripcion.SUSPENDIDA, "mora"), superAdminHeaders),
                SuscripcionResponse.class);
        BalnearioResponse detalleTrasSuspenderSuscripcion = restTemplate.exchange(
                url("/api/v1/super-admin/balnearios/" + balnearioId), HttpMethod.GET,
                new HttpEntity<>(superAdminHeaders), BalnearioResponse.class).getBody();
        // El balneario está ACTIVO pero sin suscripción vigente -> no operativo.
        assertThat(detalleTrasSuspenderSuscripcion.operativo()).isFalse();
    }

    private String extractQueryParam(String url, String param) {
        for (String part : url.split("[?&]")) {
            if (part.startsWith(param + "=")) {
                return part.substring((param + "=").length());
            }
        }
        throw new IllegalStateException("Param " + param + " no encontrado en " + url);
    }
}
