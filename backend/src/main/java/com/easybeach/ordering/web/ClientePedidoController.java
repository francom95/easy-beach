package com.easybeach.ordering.web;

import com.easybeach.ordering.service.PedidoService;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoEventoResponse;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Pedidos desde la app del cliente (etapa 13). */
@RestController
@RequestMapping("/api/v1/pedidos")
@PreAuthorize("hasRole('CLIENTE')")
public class ClientePedidoController {

    private final PedidoService pedidoService;
    private final PedidoMapper mapper;

    public ClientePedidoController(PedidoService pedidoService, PedidoMapper mapper) {
        this.pedidoService = pedidoService;
        this.mapper = mapper;
    }

    /**
     * {@code Idempotency-Key} es OBLIGATORIO (etapa 04 §1.6): la conexión en
     * la playa es mala y el reintento no debe duplicar ni pedido ni cobro.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@Valid @RequestBody CrearPedidoRequest request,
                                 @RequestHeader("Idempotency-Key") String idempotencyKey,
                                 @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(pedidoService.crear(principal.usuarioId(),
                principal.usuarioPublicId(), idempotencyKey, request));
    }

    @GetMapping
    public List<PedidoResponse> deEstadia(@RequestParam String estadiaPublicId,
                                           @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return pedidoService.misPedidosDeEstadia(principal.usuarioId(), estadiaPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{publicId}")
    public PedidoResponse obtener(@PathVariable String publicId,
                                   @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(
                pedidoService.obtenerPropioDelCliente(principal.usuarioId(), publicId));
    }

    /** Fallback de polling de ADR-003: el estado siempre es reconstruible por GET. */
    @GetMapping("/{publicId}/historial")
    public List<PedidoEventoResponse> historial(@PathVariable String publicId,
                                                 @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var pedido = pedidoService.obtenerPropioDelCliente(principal.usuarioId(), publicId);
        return pedidoService.historial(pedido.getId()).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{publicId}/cancelacion")
    public PedidoResponse cancelar(@PathVariable String publicId,
                                    @RequestBody(required = false) TransicionPedidoRequest request,
                                    @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        String motivo = request == null ? null : request.motivo();
        return mapper.toResponse(pedidoService.cancelarPorCliente(
                principal.usuarioId(), publicId, motivo));
    }
}
