package com.easybeach.platform.web;

import com.easybeach.platform.service.SuscripcionTemporadaService;
import com.easybeach.platform.web.dto.CambiarEstadoSuscripcionRequest;
import com.easybeach.platform.web.dto.SuscribirRequest;
import com.easybeach.platform.web.dto.SuscripcionResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin/balnearios/{balnearioId}/suscripciones")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminSuscripcionController {

    private final SuscripcionTemporadaService suscripcionService;

    public SuperAdminSuscripcionController(SuscripcionTemporadaService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuscripcionResponse suscribir(@PathVariable Long balnearioId, @Valid @RequestBody SuscribirRequest request,
                                          @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return suscripcionService.suscribir(principal.usuarioId(), balnearioId, request);
    }

    @GetMapping
    public List<SuscripcionResponse> listar(@PathVariable Long balnearioId) {
        return suscripcionService.listarPorBalneario(balnearioId);
    }

    @PutMapping("/{suscripcionId}/estado")
    public SuscripcionResponse cambiarEstado(@PathVariable Long balnearioId, @PathVariable Long suscripcionId,
                                              @Valid @RequestBody CambiarEstadoSuscripcionRequest request,
                                              @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return suscripcionService.cambiarEstado(principal.usuarioId(), balnearioId, suscripcionId,
                request.estado(), request.motivo());
    }
}
