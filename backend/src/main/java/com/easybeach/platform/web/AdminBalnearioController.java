package com.easybeach.platform.web;

import com.easybeach.platform.service.BalnearioService;
import com.easybeach.platform.web.dto.BalnearioResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "¿Cuál es mi propio balneario?" (etapa 17): gap real encontrado al
 * construir el panel web - ni el token de staff (solo trae
 * {@code balnearioId} numérico) ni {@code /staff/whoami} exponían
 * nombre/slug/estado para el header/sidebar del panel.
 */
@RestController
@RequestMapping("/api/v1/admin/balneario")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminBalnearioController {

    private final BalnearioService service;

    public AdminBalnearioController(BalnearioService service) {
        this.service = service;
    }

    @GetMapping
    public BalnearioResponse obtenerPropio(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.obtener(principal.balnearioId());
    }
}
