package com.easybeach.ordering.web;

import com.easybeach.ordering.service.PedidoService;
import com.easybeach.ordering.web.dto.PedidoEventoResponse;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.shared.security.EasyBeachUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cola de pedidos del panel operativo (etapa 08/17). El operador despacha; el
 * admin de balneario hereda sus capacidades (etapa 05 §2).
 */
@RestController
@RequestMapping("/api/v1/operativo/pedidos")
@PreAuthorize("hasAnyRole('OPERADOR','ADMIN_BALNEARIO')")
public class OperativoPedidoController {

    private final PedidoService pedidoService;
    private final PedidoMapper mapper;

    public OperativoPedidoController(PedidoService pedidoService, PedidoMapper mapper) {
        this.pedidoService = pedidoService;
        this.mapper = mapper;
    }

    /** Solo pedidos con pago aprobado, más antiguos primero. */
    @GetMapping("/cola")
    public List<PedidoResponse> cola(@AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return pedidoService.colaOperativa(principal.balnearioId()).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/{publicId}/estado")
    public PedidoResponse transicionar(@PathVariable String publicId,
                                        @Valid @RequestBody TransicionPedidoRequest request,
                                        @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(pedidoService.transicionarPorStaff(principal.balnearioId(),
                principal.usuarioId(), publicId, request.estado(), request.motivo()));
    }

    @PostMapping("/{publicId}/cancelacion")
    public PedidoResponse cancelar(@PathVariable String publicId,
                                    @Valid @RequestBody TransicionPedidoRequest request,
                                    @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        return mapper.toResponse(pedidoService.transicionarPorStaff(principal.balnearioId(),
                principal.usuarioId(), publicId,
                com.easybeach.ordering.domain.EstadoPedido.CANCELADO, request.motivo()));
    }

    @GetMapping("/{publicId}/historial")
    public List<PedidoEventoResponse> historial(@PathVariable String publicId,
                                                 @AuthenticationPrincipal EasyBeachUserPrincipal principal) {
        var pedido = pedidoService.colaOperativa(principal.balnearioId()).stream()
                .filter(p -> p.getPublicId().equals(publicId))
                .findFirst()
                .orElseThrow(() -> new com.easybeach.shared.error.ApiException(
                        com.easybeach.shared.error.ErrorCode.RECURSO_NO_ENCONTRADO));
        return pedidoService.historial(pedido.getId()).stream().map(mapper::toResponse).toList();
    }
}
