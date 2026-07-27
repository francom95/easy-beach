package com.easybeach.support;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.RegistroClienteRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.payments.TokenEncryptionService;
import com.easybeach.payments.domain.BalnearioMpCredencial;
import com.easybeach.payments.domain.EstadoCredencialMp;
import com.easybeach.payments.repository.BalnearioMpCredencialRepository;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.domain.EstadoSuscripcion;
import com.easybeach.platform.domain.EstadoTemporada;
import com.easybeach.platform.domain.Plan;
import com.easybeach.platform.domain.SuscripcionTemporada;
import com.easybeach.platform.domain.Temporada;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.platform.repository.PlanRepository;
import com.easybeach.platform.repository.SuscripcionTemporadaRepository;
import com.easybeach.platform.repository.TemporadaRepository;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Arma escenarios completos de prueba (balneario OPERATIVO + staff + cliente)
 * sin repetir 40 líneas de seed en cada clase de test. "Operativo" implica
 * suscripción ACTIVA en temporada EN_CURSO - sin eso, el balneario no acepta
 * estadías (etapa 10).
 *
 * <p>Usa <b>rutas relativas</b>: el {@code TestRestTemplate} de
 * {@code webEnvironment=RANDOM_PORT} ya las resuelve contra el puerto real.
 * No se puede usar {@code @LocalServerPort} acá - este bean se construye
 * antes de que el servidor arranque y la property todavía no existe.
 */
@Component
public class EscenarioBalneario {

    private static final String PASSWORD = "ClaveSegura123";

    private final BalnearioRepository balnearioRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    private final RolRepository rolRepository;
    private final PlanRepository planRepository;
    private final TemporadaRepository temporadaRepository;
    private final SuscripcionTemporadaRepository suscripcionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TestRestTemplate restTemplate;
    private final BalnearioMpCredencialRepository credencialRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public EscenarioBalneario(BalnearioRepository balnearioRepository, UsuarioRepository usuarioRepository,
                               UsuarioBalnearioRolRepository usuarioBalnearioRolRepository,
                               RolRepository rolRepository, PlanRepository planRepository,
                               TemporadaRepository temporadaRepository,
                               SuscripcionTemporadaRepository suscripcionRepository,
                               PasswordEncoder passwordEncoder, TestRestTemplate restTemplate,
                               BalnearioMpCredencialRepository credencialRepository,
                               TokenEncryptionService tokenEncryptionService) {
        this.balnearioRepository = balnearioRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioBalnearioRolRepository = usuarioBalnearioRolRepository;
        this.rolRepository = rolRepository;
        this.planRepository = planRepository;
        this.temporadaRepository = temporadaRepository;
        this.suscripcionRepository = suscripcionRepository;
        this.passwordEncoder = passwordEncoder;
        this.restTemplate = restTemplate;
        this.credencialRepository = credencialRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    public record Contexto(Long balnearioId, String slug, HttpHeaders adminHeaders,
                            HttpHeaders carperoHeaders, Long carperoId,
                            HttpHeaders operadorHeaders, Long operadorId) {
    }

    public Contexto crearBalnearioOperativoConStaff(String slugPrefix) {
        Balneario balneario = new Balneario();
        String slug = slugPrefix + "-" + System.nanoTime();
        balneario.setSlug(slug);
        balneario.setNombre(slugPrefix);
        balneario.setEmailContacto(slugPrefix + "@example.com");
        balneario = balnearioRepository.save(balneario);

        darDeAltaSuscripcionVigente(balneario.getId());
        vincularMercadoPago(balneario.getId());

        Usuario admin = crearStaff(balneario.getId(), RolCodigo.ADMIN_BALNEARIO, "admin");
        Usuario carpero = crearStaff(balneario.getId(), RolCodigo.CARPERO, "carpero");
        Usuario operador = crearStaff(balneario.getId(), RolCodigo.OPERADOR, "operador");

        return new Contexto(balneario.getId(), slug, loginHeaders(admin.getEmail(), "/api/v1/auth/login/staff"),
                loginHeaders(carpero.getEmail(), "/api/v1/auth/login/staff"), carpero.getId(),
                loginHeaders(operador.getEmail(), "/api/v1/auth/login/staff"), operador.getId());
    }

    /** Sin credencial MP vinculada el balneario no puede cobrar (etapa 10). */
    private void vincularMercadoPago(Long balnearioId) {
        BalnearioMpCredencial credencial = new BalnearioMpCredencial();
        credencial.setBalnearioId(balnearioId);
        credencial.setMpUserId("fake-mp-user-" + balnearioId);
        credencial.setAccessTokenCifrado(tokenEncryptionService.encrypt("fake-access-token"));
        credencial.setRefreshTokenCifrado(tokenEncryptionService.encrypt("fake-refresh-token"));
        credencial.setTokenExpiraAt(Instant.now().plus(Duration.ofDays(30)));
        credencial.setEstado(EstadoCredencialMp.VINCULADA);
        credencialRepository.save(credencial);
    }

    public HttpHeaders registrarClienteYObtenerHeaders() {
        String email = "cliente." + System.nanoTime() + "@example.com";
        restTemplate.postForEntity("/api/v1/auth/registro",
                new RegistroClienteRequest(email, PASSWORD, "Cliente de prueba"), TokenResponse.class);
        return loginHeaders(email, "/api/v1/auth/login/cliente");
    }

    /**
     * Reutiliza la temporada {@code EN_CURSO} si ya existe una en este
     * contexto de test, en vez de crear una por balneario: en producción hay
     * UNA temporada compartida por todos los balnearios (etapa 03 §3.1), y
     * el reporte de plataforma (etapa 15) asume exactamente eso al buscar
     * "la" temporada en curso.
     */
    private void darDeAltaSuscripcionVigente(Long balnearioId) {
        Plan plan = new Plan();
        plan.setNombre("Plan de prueba " + System.nanoTime());
        plan.setPrecio(new BigDecimal("10000.00"));
        plan.setActivo(true);
        plan = planRepository.save(plan);

        Temporada temporada = temporadaRepository.findByEstado(EstadoTemporada.EN_CURSO).stream()
                .findFirst()
                .orElseGet(() -> {
                    Temporada nueva = new Temporada();
                    nueva.setNombre("Temporada de prueba " + System.nanoTime());
                    nueva.setFechaInicio(LocalDate.now().minusDays(1));
                    nueva.setFechaFin(LocalDate.now().plusMonths(3));
                    nueva.setEstado(EstadoTemporada.EN_CURSO);
                    return temporadaRepository.save(nueva);
                });

        SuscripcionTemporada suscripcion = new SuscripcionTemporada();
        suscripcion.setBalnearioId(balnearioId);
        suscripcion.setPlan(plan);
        suscripcion.setTemporada(temporada);
        suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
        suscripcionRepository.save(suscripcion);
    }

    private Usuario crearStaff(Long balnearioId, RolCodigo rol, String prefijo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(prefijo + "." + System.nanoTime() + "@example.com");
        usuario.setPasswordHash(passwordEncoder.encode(PASSWORD));
        usuario.setNombre(prefijo);
        usuario.setTipo(TipoUsuario.STAFF);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(usuario);
        vinculo.setBalnearioId(balnearioId);
        vinculo.setRol(rolRepository.findByCodigo(rol).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);
        return usuario;
    }

    private HttpHeaders loginHeaders(String email, String loginPath) {
        TokenResponse tokens = restTemplate.postForEntity(loginPath,
                new LoginRequest(email, PASSWORD), TokenResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        return headers;
    }
}
