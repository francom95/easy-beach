package com.easybeach.payments.web;

import com.easybeach.payments.service.BalnearioMpCredencialService;
import com.easybeach.payments.web.dto.EstadoVinculacionResponse;
import com.easybeach.payments.web.dto.IniciarVinculacionResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Vinculación OAuth de Mercado Pago del propio balneario del admin logueado (ADR-004). */
@RestController
@RequestMapping("/api/v1/admin/mercadopago")
@PreAuthorize("hasRole('ADMIN_BALNEARIO')")
public class AdminMercadoPagoController {

    private final BalnearioMpCredencialService service;

    public AdminMercadoPagoController(BalnearioMpCredencialService service) {
        this.service = service;
    }

    @GetMapping("/estado")
    public EstadoVinculacionResponse estado(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var estado = service.obtenerEstado(principal.balnearioId());
        return new EstadoVinculacionResponse(estado.vinculado(), estado.mpUserId(), estado.estado().name());
    }

    @GetMapping("/oauth/iniciar")
    public IniciarVinculacionResponse iniciar(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return new IniciarVinculacionResponse(service.iniciarVinculacion(principal.balnearioId()));
    }

    @PostMapping("/desvincular")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desvincular(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        service.desvincular(principal.balnearioId());
    }
}
