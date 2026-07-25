package com.easybeach.identity.web;

import com.easybeach.identity.domain.UsuarioBalnearioRol;
import com.easybeach.identity.security.EasyBeachUserPrincipal;
import com.easybeach.identity.service.UsuarioBalnearioRolService;
import com.easybeach.identity.web.dto.MiembroResponse;
import com.easybeach.identity.web.dto.WhoAmIResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints mínimos de demostración de la etapa 09 (autorización por rol +
 * aislamiento multitenant). Los ABMs reales del staff llegan en las etapas
 * 10-15; esto solo prueba que la infraestructura fundacional funciona.
 */
@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasAnyRole('CARPERO','OPERADOR','ADMIN_BALNEARIO')")
public class StaffController {

    private final UsuarioBalnearioRolService usuarioBalnearioRolService;

    public StaffController(UsuarioBalnearioRolService usuarioBalnearioRolService) {
        this.usuarioBalnearioRolService = usuarioBalnearioRolService;
    }

    @GetMapping("/whoami")
    public WhoAmIResponse whoAmI(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return new WhoAmIResponse(
                principal.usuarioPublicId(),
                principal.tipo().name(),
                principal.rol().name(),
                principal.balnearioId()
        );
    }

    @GetMapping("/miembros")
    public List<MiembroResponse> miembros() {
        return usuarioBalnearioRolService.listarMiembrosDelBalnearioActual().stream()
                .map(m -> new MiembroResponse(
                        m.getUsuario().getPublicId(),
                        m.getUsuario().getNombre(),
                        m.getRol().getCodigo().name(),
                        m.getBalnearioId()))
                .toList();
    }
}
