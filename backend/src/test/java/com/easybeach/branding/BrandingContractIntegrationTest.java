package com.easybeach.branding;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.branding.service.ConfiguracionVisualService;
import com.easybeach.branding.theming.ThemeTokenKeys;
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
import com.easybeach.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class BrandingContractIntegrationTest extends AbstractIntegrationTest {

    private static final List<String> TODAS_LAS_CLAVES = List.of(
            ThemeTokenKeys.CONTRACT, ThemeTokenKeys.NAME,
            ThemeTokenKeys.COLOR_PRIMARY, ThemeTokenKeys.COLOR_ON_PRIMARY,
            ThemeTokenKeys.COLOR_SECONDARY, ThemeTokenKeys.COLOR_ON_SECONDARY,
            ThemeTokenKeys.COLOR_BACKGROUND, ThemeTokenKeys.COLOR_ON_BACKGROUND,
            ThemeTokenKeys.COLOR_SURFACE, ThemeTokenKeys.COLOR_ON_SURFACE, ThemeTokenKeys.COLOR_ON_SURFACE_MUTED,
            ThemeTokenKeys.COLOR_BORDER,
            ThemeTokenKeys.COLOR_SUCCESS, ThemeTokenKeys.COLOR_ON_SUCCESS,
            ThemeTokenKeys.COLOR_WARNING, ThemeTokenKeys.COLOR_ON_WARNING,
            ThemeTokenKeys.COLOR_ERROR, ThemeTokenKeys.COLOR_ON_ERROR,
            ThemeTokenKeys.COLOR_INFO, ThemeTokenKeys.COLOR_ON_INFO,
            ThemeTokenKeys.TYPOGRAPHY_FAMILY, ThemeTokenKeys.TYPOGRAPHY_SCALE,
            ThemeTokenKeys.ASSET_LOGO, ThemeTokenKeys.ASSET_LOGO_COMPACT, ThemeTokenKeys.ASSET_COVER,
            ThemeTokenKeys.ASSET_SPLASH, ThemeTokenKeys.ASSET_PRODUCT_PLACEHOLDER
    );

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
    @Autowired
    private ConfiguracionVisualService configuracionVisualService;

    private String slug;
    private HttpHeaders adminHeaders;

    @BeforeEach
    void seedBalnearioConAdmin() {
        Balneario balneario = new Balneario();
        slug = "balneario-branding-" + System.nanoTime();
        balneario.setSlug(slug);
        balneario.setNombre("Balneario Branding");
        balneario.setEmailContacto("contacto@balneario.com");
        balneario = balnearioRepository.save(balneario);

        // Sembrado manual del default (en el flujo real lo hace BalnearioCreadoListener al crear vía API).
        configuracionVisualService.sembrarDefault(balneario.getId());

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

    @SuppressWarnings("unchecked")
    @Test
    void endpointPublicoDevuelveTodosLosTokensDelContrato() {
        Map<String, Object> tokens = restTemplate.getForObject(url("/api/v1/balnearios/" + slug + "/branding"), Map.class);
        assertThat(tokens.keySet()).containsExactlyInAnyOrderElementsOf(TODAS_LAS_CLAVES);
        // Los on-* nunca los elige el balneario - deben venir presentes y ser hex válidos.
        assertThat(tokens.get(ThemeTokenKeys.COLOR_ON_PRIMARY)).asString().matches("^#[0-9A-Fa-f]{6}$");
    }

    @Test
    void subirLogoValidoFuncionaYQuedaAccesiblePorUrlPublica() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(pngMinimo()) {
            @Override
            public String getFilename() {
                return "logo.png";
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(adminHeaders);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = restTemplate.exchange(url("/api/v1/admin/branding/assets/LOGO"), HttpMethod.POST,
                new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String logoUrl = (String) response.getBody().get(ThemeTokenKeys.ASSET_LOGO);
        assertThat(logoUrl).startsWith("/public/assets/balnearios/");

        var fetched = restTemplate.getForEntity(url(logoUrl), byte[].class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isEqualTo(pngMinimo());
    }

    @Test
    void archivoQueNoEsUnaImagenRealEsRechazadoAunqueDigaSerPng() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("esto no es una imagen, es texto plano".getBytes()) {
            @Override
            public String getFilename() {
                return "logo.png"; // extensión mentirosa - la validación es por magic bytes, no por nombre
            }
        });
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(adminHeaders);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        var response = restTemplate.exchange(url("/api/v1/admin/branding/assets/LOGO"), HttpMethod.POST,
                new HttpEntity<>(body, headers), ProblemDetail.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
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
