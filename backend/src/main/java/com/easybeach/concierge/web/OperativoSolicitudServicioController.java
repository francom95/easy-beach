package com.easybeach.concierge.web;

import com.easybeach.concierge.service.SolicitudServicioService;
import com.easybeach.concierge.web.dto.SolicitudServicioResponse;
import com.easybeach.concierge.web.dto.TransicionSolicitudRequest;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cola de solicitudes de servicio del carpero (etapa 14). */
@RestController
@RequestMapping("/api/v1/operativo/solicitudes-servicio")
@PreAuthorize("hasAnyRole('CARPERO','ADMIN_BALNEARIO')")
public class OperativoSolicitudServicioController {

    private final SolicitudServicioService service;
    private final SolicitudServicioMapper mapper;

    public OperativoSolicitudServicioController(SolicitudServicioService service, SolicitudServicioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/cola")
    public List<SolicitudServicioResponse> cola(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return service.colaOperativa(principal.balnearioId()).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/{publicId}/estado")
    public SolicitudServicioResponse transicionar(@PathVariable String publicId,
                                                   @Valid @RequestBody TransicionSolicitudRequest request,
                                                   @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(service.transicionar(principal.balnearioId(), principal.usuarioId(),
                publicId, request.estado()));
    }
}
