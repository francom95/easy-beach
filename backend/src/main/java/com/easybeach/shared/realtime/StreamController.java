package com.easybeach.shared.realtime;

import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Endpoints SSE de ADR-003. La suscripción se acota por el token, nunca por parámetro. */
@RestController
@RequestMapping("/api/v1/stream")
public class StreamController {

    private final TiempoRealService tiempoRealService;

    public StreamController(TiempoRealService tiempoRealService) {
        this.tiempoRealService = tiempoRealService;
    }

    /** Eventos del cliente autenticado: {@code pago.resultado}, {@code pedido.estado}. */
    @GetMapping(value = "/cliente", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public SseEmitter cliente(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return tiempoRealService.suscribir(CanalTiempoReal.CLIENTE, principal.usuarioPublicId());
    }

    /** Eventos del balneario del staff autenticado: {@code pedido.nuevo}, {@code pedido.estado}. */
    @GetMapping(value = "/operativo", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('CARPERO','OPERADOR','ADMIN_BALNEARIO')")
    public SseEmitter operativo(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        if (principal.balnearioId() == null) {
            throw new ApiException(ErrorCode.ACCESO_DENEGADO, "El token no tiene balneario asociado");
        }
        return tiempoRealService.suscribir(CanalTiempoReal.OPERATIVO, String.valueOf(principal.balnearioId()));
    }
}
