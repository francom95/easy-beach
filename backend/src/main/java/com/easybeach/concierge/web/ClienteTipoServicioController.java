package com.easybeach.concierge.web;

import com.easybeach.concierge.service.TipoServicioService;
import com.easybeach.concierge.web.dto.TipoServicioResponse;
import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import com.easybeach.shared.tenancy.TenantContext;
import com.easybeach.stay.repository.EstadiaRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qué puede pedirle el cliente al carpero, para el balneario de SU propia
 * estadía. Resuelve el balneario desde la estadía (vía {@code stay}, ya
 * permitido por ADR-002) en vez de por slug: {@code concierge} no depende
 * de {@code platform}, y de paso esto ya viene con ownership-check gratis
 * (solo balnearios donde el cliente tiene una estadía real).
 */
@RestController
@RequestMapping("/api/v1/tipos-servicio")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteTipoServicioController {

    private final TipoServicioService tipoServicioService;
    private final EstadiaRepository estadiaRepository;

    public ClienteTipoServicioController(TipoServicioService tipoServicioService,
                                          EstadiaRepository estadiaRepository) {
        this.tipoServicioService = tipoServicioService;
        this.estadiaRepository = estadiaRepository;
    }

    @GetMapping
    public List<TipoServicioResponse> listar(@RequestParam String estadiaPublicId,
                                              @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        Long balnearioId = estadiaRepository.findByPublicId(estadiaPublicId)
                .filter(e -> e.getClienteId().equals(principal.usuarioId()))
                .orElseThrow(() -> new ApiException(ErrorCode.RECURSO_NO_ENCONTRADO))
                .getBalnearioId();
        // El token del cliente no trae balnearioId (etapa 05 §1.2: puede tener
        // estadías en varios balnearios); se setea recién acá, tras resolverlo
        // desde SU estadía - mismo patrón que EstadiaService.solicitar().
        TenantContext.set(balnearioId);
        return tipoServicioService.listarActivos(balnearioId).stream()
                .map(t -> new TipoServicioResponse(t.getId(), t.getNombre(), t.isActivo(), t.getOrden()))
                .toList();
    }
}
