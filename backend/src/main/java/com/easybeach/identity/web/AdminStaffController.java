package com.easybeach.identity.web;

import com.easybeach.identity.service.UsuarioBalnearioRolService;
import com.easybeach.identity.web.dto.InvitarStaffRequest;
import com.easybeach.identity.web.dto.InvitarStaffResponse;
import com.easybeach.identity.web.dto.MiembroResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ABM real de staff del propio balneario (etapa 17): {@code /staff/miembros}
 * (etapa 09) era solo de lectura, sin invitar/revocar - gap real encontrado
 * al diseñar el panel admin (pantalla "Staff" del mockup de etapa 08).
 */
@RestController
@RequestMapping("/api/v1/admin/staff")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminStaffController {

    private final UsuarioBalnearioRolService service;

    public AdminStaffController(UsuarioBalnearioRolService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitarStaffResponse invitar(@Valid @RequestBody InvitarStaffRequest request,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.invitar(principal.balnearioId(), request);
    }

    @GetMapping
    public List<MiembroResponse> listar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.listarStaff(principal.balnearioId());
    }

    @DeleteMapping("/{usuarioPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocar(@PathVariable String usuarioPublicId, @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.revocar(principal.balnearioId(), usuarioPublicId);
    }
}
