package com.easybeach.stay.web;

import com.easybeach.shared.security.EasyBeachUserPrincipal;
import com.easybeach.stay.service.EstadiaService;
import com.easybeach.stay.web.dto.EstadiaPendienteResponse;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.RechazarEstadiaRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bandeja de validación de estadías del panel operativo (etapa 08/17). El
 * carpero valida; el admin de balneario hereda sus capacidades operativas
 * (etapa 05 §2).
 */
@RestController
@RequestMapping("/api/v1/operativo/estadias")
@PreAuthorize("hasAnyRole('CARPERO','ADMIN_BALNEARIO')")
public class OperativoEstadiaController {

    private final EstadiaService estadiaService;

    public OperativoEstadiaController(EstadiaService estadiaService) {
        this.estadiaService = estadiaService;
    }

    @GetMapping("/pendientes")
    public List<EstadiaPendienteResponse> pendientes(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.pendientesDeValidacion(principal.balnearioId());
    }

    @PostMapping("/{publicId}/validacion")
    public EstadiaResponse validar(@PathVariable String publicId,
                                    @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.validar(principal.balnearioId(),
                principal.usuarioId(), publicId);
    }

    @PostMapping("/{publicId}/rechazo")
    public EstadiaResponse rechazar(@PathVariable String publicId,
                                     @Valid @RequestBody RechazarEstadiaRequest request,
                                     @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.rechazar(principal.balnearioId(),
                principal.usuarioId(), publicId, request.motivo());
    }
}
