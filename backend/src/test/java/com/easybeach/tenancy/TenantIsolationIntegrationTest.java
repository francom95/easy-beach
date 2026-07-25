package com.easybeach.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.easybeach.identity.domain.EstadoUsuario;
import com.easybeach.shared.security.RolCodigo;
import com.easybeach.shared.security.TipoUsuario;
import com.easybeach.identity.domain.Usuario;
import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.repository.RolRepository;
import com.easybeach.identity.repository.UsuarioBalnearioRolRepository;
import com.easybeach.identity.repository.UsuarioRepository;
import com.easybeach.identity.service.UsuarioBalnearioRolService;
import com.easybeach.platform.domain.Balneario;
import com.easybeach.platform.repository.BalnearioRepository;
import com.easybeach.shared.tenancy.TenantContext;
import com.easybeach.shared.tenancy.TenantContextMissingException;
import com.easybeach.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prueba directa del ADR-001 / criterio de aceptación de la etapa 09:
 * "test que demuestra que una query sin tenant falla o filtra" y aislamiento
 * cross-tenant en el mismo mecanismo.
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioBalnearioRolService usuarioBalnearioRolService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private UsuarioBalnearioRolRepository usuarioBalnearioRolRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private BalnearioRepository balnearioRepository;

    @AfterEach
    void limpiarContexto() {
        TenantContext.clear();
    }

    @Test
    void unaQuerySinTenantContextFalla() {
        TenantContext.clear();
        assertThatThrownBy(() -> usuarioBalnearioRolService.listarMiembrosDelBalnearioActual())
                .isInstanceOf(TenantContextMissingException.class);
    }

    @Test
    void conTenantContextSoloVeSuPropioBalneario() {
        Balneario balnearioA = crearBalneario("balneario-a");
        Balneario balnearioB = crearBalneario("balneario-b");
        crearMiembro(balnearioA, "carpero.a");
        crearMiembro(balnearioA, "operador.a");
        crearMiembro(balnearioB, "carpero.b");

        TenantContext.set(balnearioA.getId());
        var miembros = usuarioBalnearioRolService.listarMiembrosDelBalnearioActual();

        assertThat(miembros).hasSize(2);
        assertThat(miembros).allMatch(m -> m.getBalnearioId().equals(balnearioA.getId()));
    }

    private Balneario crearBalneario(String slugPrefix) {
        Balneario balneario = new Balneario();
        balneario.setSlug(slugPrefix + "-" + System.nanoTime());
        balneario.setNombre(slugPrefix);
        balneario.setEmailContacto(slugPrefix + "@example.com");
        return balnearioRepository.save(balneario);
    }

    private void crearMiembro(Balneario balneario, String emailPrefix) {
        Usuario usuario = new Usuario();
        usuario.setEmail(emailPrefix + "." + System.nanoTime() + "@example.com");
        usuario.setPasswordHash("irrelevante-para-este-test");
        usuario.setNombre(emailPrefix);
        usuario.setTipo(TipoUsuario.STAFF);
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        UsuarioBalnearioRol vinculo = new UsuarioBalnearioRol();
        vinculo.setUsuario(usuario);
        vinculo.setBalnearioId(balneario.getId());
        vinculo.setRol(rolRepository.findByCodigo(RolCodigo.CARPERO).orElseThrow());
        usuarioBalnearioRolRepository.save(vinculo);
    }
}
