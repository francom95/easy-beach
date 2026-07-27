package com.easybeach.stay.web;

import com.easybeach.identity.web.CurrentUserResolver;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import com.easybeach.stay.service.EstadiaService;
import com.easybeach.stay.web.dto.CambiarUbicacionRequest;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.ResumenCierreResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
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

/** Estadía desde la app del cliente (etapa 12). */
@RestController
@RequestMapping("/api/v1/estadias")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteEstadiaController {

    private final EstadiaService estadiaService;
    private final CurrentUserResolver currentUserResolver;

    public ClienteEstadiaController(EstadiaService estadiaService, CurrentUserResolver currentUserResolver) {
        this.estadiaService = estadiaService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstadiaResponse solicitar(@Valid @RequestBody SolicitarEstadiaRequest request,
                                      @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.solicitar(currentUserResolver.resolveId(principal),
                request.balnearioSlug(), request.ubicacionId());
    }

    /** Las que ocupan cupo (pendientes o activas), en todos los balnearios. */
    @GetMapping("/vigentes")
    public List<EstadiaResponse> vigentes(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.misEstadiasVigentes(currentUserResolver.resolveId(principal));
    }

    @GetMapping("/historial")
    public List<EstadiaResponse> historial(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.miHistorial(currentUserResolver.resolveId(principal));
    }

    @PutMapping("/{publicId}/ubicacion")
    public EstadiaResponse cambiarUbicacion(@PathVariable String publicId,
                                             @Valid @RequestBody CambiarUbicacionRequest request,
                                             @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.cambiarUbicacion(currentUserResolver.resolveId(principal), publicId,
                request.ubicacionId());
    }

    @PostMapping("/{publicId}/cierre")
    public ResumenCierreResponse cerrar(@PathVariable String publicId,
                                         @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return estadiaService.cerrar(currentUserResolver.resolveId(principal), publicId);
    }
}
