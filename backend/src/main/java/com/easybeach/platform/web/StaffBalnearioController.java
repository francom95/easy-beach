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
 * "¿En qué balneario estoy?" para CUALQUIER staff (etapa 17) - a diferencia
 * de {@link AdminBalnearioController} (solo ADMIN_BALNEARIO), el header del
 * panel operativo (CARPERO/OPERADOR) también necesita nombre/slug/estado
 * del balneario, y el token solo trae {@code balnearioId} numérico.
 *
 * <p>Vive en {@code platform.web} (no {@code identity.web}, donde vive el
 * resto de {@code /api/v1/staff/**}): el módulo {@code identity} no puede
 * depender de {@code platform} (ADR-002, verificado por
 * {@code ModuleDependencyRulesTest}) - un primer intento de agregar esto a
 * {@code StaffController} rompió esa regla.
 */
@RestController
@RequestMapping("/api/v1/staff/balneario")
@PreAuthorize("hasAnyRole('CARPERO','OPERADOR','ADMIN_BALNEARIO')")
public class StaffBalnearioController {

    private final BalnearioService service;

    public StaffBalnearioController(BalnearioService service) {
        this.service = service;
    }

    @GetMapping
    public BalnearioResponse miBalneario(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.obtener(principal.balnearioId());
    }
}
