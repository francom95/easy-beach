package com.easybeach.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.shared.security.RolCodigo;
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
import com.easybeach.platform.web.dto.CrearBalnearioRequest;
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

/** Etapa 10 criterio de aceptación: "los endpoints de Super Admin son inaccesibles para cualquier otro rol". */
class SuperAdminAccessControlIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private BalnearioRepository balnearioRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminBalnearioEmail;

    @BeforeEach
    void seedAdminDeBalneario() {
        Balneario balneario = new Balneario();
        balneario.setSlug("balneario-" + System.nanoTime());
        balneario.setNombre("Balneario de prueba");
        balneario.setEmailContacto("contacto@balneario.com");
        balneario = balnearioRepository.save(balneario);

        Usuario admin = new Usuario();
        adminBalnearioEmail = "admin." + System.nanoTime() + "@balneario.com";
        admin.setEmail(adminBalnearioEmail);
        admin.setPasswordHash(passwordEncoder.encode("ClaveSegura123"));
        admin.setNombre("Admin de balneario");
        admin.setTipo(TipoUsuario.STAFF);
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin = usuarioRepository.save(admin);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(admin);
        vinculo.setBalnearioId(balneario.getId());
        vinculo.setRol(rolRepository.findByCodigo(RolCodigo.ADMIN_BALNEARIO).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);
    }

    @Test
    void adminDeBalnearioNoPuedeCrearBalnearios() {
        TokenResponse tokens = restTemplate.postForEntity(url("/api/v1/auth/login/staff"),
                new LoginRequest(adminBalnearioEmail, "ClaveSegura123"), TokenResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        var request = new CrearBalnearioRequest("otro-balneario-" + System.nanoTime(), "Otro", "x@x.com", null,
                "Admin", "admin2@x.com");
        var response = restTemplate.exchange(url("/api/v1/super-admin/balnearios"), HttpMethod.POST,
                new HttpEntity<>(request, headers), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void clienteNoPuedeListarBalneariosDeSuperAdmin() {
        String email = "cliente." + System.nanoTime() + "@example.com";
        restTemplate.postForEntity(url("/api/v1/auth/registro"),
                new com.easybeach.identity.web.dto.RegistroClienteRequest(email, "ClaveSegura123", "Cliente"),
                TokenResponse.class);
        TokenResponse tokens = restTemplate.postForEntity(url("/api/v1/auth/login/cliente"),
                new LoginRequest(email, "ClaveSegura123"), TokenResponse.class).getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());

        var response = restTemplate.exchange(url("/api/v1/super-admin/balnearios"), HttpMethod.GET,
                new HttpEntity<>(headers), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void sinTokenEsRechazado() {
        var response = restTemplate.exchange(url("/api/v1/super-admin/balnearios"), HttpMethod.GET,
                null, ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
