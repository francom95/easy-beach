package com.easybeach.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.identity.domain.RolCodigo;
import com.easybeach.identity.domain.TipoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.web.dto.LoginRequest;
import com.easybeach.identity.web.dto.RefreshRequest;
import com.easybeach.identity.web.dto.RegistroClienteRequest;
import com.easybeach.identity.web.dto.TokenResponse;
import com.easybeach.identity.web.dto.WhoAmIResponse;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

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

    private Balneario balneario;

    @BeforeEach
    void seedStaffUser() {
        balneario = new Balneario();
        balneario.setSlug("costa-nena-" + System.nanoTime());
        balneario.setNombre("Costa Nena");
        balneario.setEmailContacto("admin@costanena.com.ar");
        balneario = balnearioRepository.save(balneario);

        Usuario staff = new Usuario();
        staff.setEmail("carpero." + System.nanoTime() + "@costanena.com.ar");
        staff.setPasswordHash(passwordEncoder.encode("ClaveSegura123"));
        staff.setNombre("Carpero de prueba");
        staff.setTipo(TipoUsuario.STAFF);
        staff.setEstado(EstadoUsuario.ACTIVO);
        staff = usuarioRepository.save(staff);
        this.staffEmail = staff.getEmail();

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(staff);
        vinculo.setBalnearioId(balneario.getId());
        vinculo.setRol(rolRepository.findByCodigo(RolCodigo.CARPERO).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);
    }

    private String staffEmail;

    @Test
    void registroYLoginDeCliente() {
        String email = "cliente." + System.nanoTime() + "@example.com";
        var registro = new RegistroClienteRequest(email, "ClaveSegura123", "Cliente de Prueba");

        ResponseEntity<TokenResponse> registroResponse =
                restTemplate.postForEntity(url("/api/v1/auth/registro"), registro, TokenResponse.class);
        assertThat(registroResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registroResponse.getBody().accessToken()).isNotBlank();
        assertThat(registroResponse.getBody().balnearioId()).isNull();

        var login = new LoginRequest(email, "ClaveSegura123");
        ResponseEntity<TokenResponse> loginResponse =
                restTemplate.postForEntity(url("/api/v1/auth/login/cliente"), login, TokenResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().tipo()).isEqualTo("CLIENTE");
    }

    @Test
    void loginDeStaffIncluyeBalnearioIdYRol() {
        var login = new LoginRequest(staffEmail, "ClaveSegura123");
        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity(url("/api/v1/auth/login/staff"), login, TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().rol()).isEqualTo("CARPERO");
        assertThat(response.getBody().balnearioId()).isEqualTo(balneario.getId());
    }

    @Test
    void refreshRotaElTokenYElAnteriorYaNoSirve() {
        var login = new LoginRequest(staffEmail, "ClaveSegura123");
        TokenResponse original = restTemplate.postForEntity(
                url("/api/v1/auth/login/staff"), login, TokenResponse.class).getBody();

        var refreshRequest = new RefreshRequest(original.refreshToken());
        ResponseEntity<TokenResponse> refreshed =
                restTemplate.postForEntity(url("/api/v1/auth/refresh"), refreshRequest, TokenResponse.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody().accessToken()).isNotEqualTo(original.accessToken());
        assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(original.refreshToken());

        // Reuso del refresh ya rotado: se rechaza (401).
        ResponseEntity<ProblemDetail> reuse =
                restTemplate.postForEntity(url("/api/v1/auth/refresh"), refreshRequest, ProblemDetail.class);
        assertThat(reuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // La familia completa quedó revocada: el token nuevo (emitido en la
        // rotación) tampoco sirve más, aunque nunca se haya reusado él mismo.
        var refreshDelRotado = new RefreshRequest(refreshed.getBody().refreshToken());
        ResponseEntity<ProblemDetail> tras_revocacion =
                restTemplate.postForEntity(url("/api/v1/auth/refresh"), refreshDelRotado, ProblemDetail.class);
        assertThat(tras_revocacion.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevocaElRefreshToken() {
        var login = new LoginRequest(staffEmail, "ClaveSegura123");
        TokenResponse tokens = restTemplate.postForEntity(
                url("/api/v1/auth/login/staff"), login, TokenResponse.class).getBody();

        restTemplate.postForEntity(url("/api/v1/auth/logout"), new RefreshRequest(tokens.refreshToken()), Void.class);

        ResponseEntity<ProblemDetail> afterLogout = restTemplate.postForEntity(
                url("/api/v1/auth/refresh"), new RefreshRequest(tokens.refreshToken()), ProblemDetail.class);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accesoDenegadoPorRol_clienteNoPuedeEntrarAEndpointDeStaff() {
        String email = "cliente." + System.nanoTime() + "@example.com";
        restTemplate.postForEntity(url("/api/v1/auth/registro"),
                new RegistroClienteRequest(email, "ClaveSegura123", "Cliente"), TokenResponse.class);
        TokenResponse clienteTokens = restTemplate.postForEntity(url("/api/v1/auth/login/cliente"),
                new LoginRequest(email, "ClaveSegura123"), TokenResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(clienteTokens.accessToken());
        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                url("/api/v1/staff/whoami"), org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void staffPuedeAccederASuPropioWhoAmI() {
        TokenResponse tokens = restTemplate.postForEntity(url("/api/v1/auth/login/staff"),
                new LoginRequest(staffEmail, "ClaveSegura123"), TokenResponse.class).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<WhoAmIResponse> response = restTemplate.exchange(
                url("/api/v1/staff/whoami"), org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(headers), WhoAmIResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().rol()).isEqualTo("CARPERO");
        assertThat(response.getBody().balnearioId()).isEqualTo(balneario.getId());
    }
}
