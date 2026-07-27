package com.easybeach.concierge.web;

import com.easybeach.concierge.service.SolicitudServicioService;
import com.easybeach.concierge.web.dto.SolicitarServicioRequest;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Solicitud de servicio desde la app del cliente (etapa 14). */
@RestController
@RequestMapping("/api/v1/solicitudes-servicio")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteSolicitudServicioController {

    private final SolicitudServicioService service;
    private final SolicitudServicioMapper mapper;

    public ClienteSolicitudServicioController(SolicitudServicioService service, SolicitudServicioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudServicioResponse solicitar(@Valid @RequestBody SolicitarServicioRequest request,
                                                @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(service.solicitar(principal.usuarioId(), principal.usuarioPublicId(),
                request.estadiaPublicId(), request.tipoServicioId(), request.nota()));
    }

    @GetMapping
    public List<SolicitudServicioResponse> deEstadia(@RequestParam String estadiaPublicId,
                                                       @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.deEstadia(principal.usuarioId(), estadiaPublicId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{publicId}/cancelacion")
    public SolicitudServicioResponse cancelar(@PathVariable String publicId,
                                               @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(service.cancelarPorCliente(principal.usuarioId(), publicId));
    }
}
