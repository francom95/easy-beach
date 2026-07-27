package com.easybeach.ordering.web;

import com.easybeach.ordering.domain.Pedido;
import com.easybeach.ordering.domain.PedidoEvento;
import com.easybeach.ordering.web.dto.PedidoEventoResponse;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.stay.repository.UbicacionRepository;
import org.springframework.stereotype.Component;

@Component
public class PedidoMapper {

    private final UbicacionRepository ubicacionRepository;

    public PedidoMapper(UbicacionRepository ubicacionRepository) {
        this.ubicacionRepository = ubicacionRepository;
    }

    public PedidoResponse toResponse(Pedido pedido) {
        String ubicacion = ubicacionRepository.findById(pedido.getUbicacionId())
                .map(u -> u.getIdentificador())
                .orElse(null);
        return new PedidoResponse(
                pedido.getPublicId(),
                pedido.getBalnearioId(),
                ubicacion,
                pedido.getEstado().name(),
                pedido.getSubtotal(),
                pedido.getDescuentoTotal(),
                pedido.getTotal(),
                pedido.getItems().stream()
                        .map(i -> new PedidoResponse.ItemResponse(i.getNombreProducto(), i.getNombreVariante(),
                                i.getPrecioUnitario(), i.getCantidad(), i.getSubtotalLinea()))
                        .toList(),
                pedido.getPromociones().stream()
                        .map(p -> new PedidoResponse.PromocionAplicadaResponse(p.getNombrePromocion(),
                                p.getMontoDescuento()))
                        .toList(),
                pedido.getMotivoCancelacion(),
                pedido.getCreatedAt());
    }

    public PedidoEventoResponse toResponse(PedidoEvento evento) {
        return new PedidoEventoResponse(
                evento.getEstadoAnterior() == null ? null : evento.getEstadoAnterior().name(),
                evento.getEstadoNuevo().name(),
                evento.getActorTipo(),
                evento.getMotivo(),
                evento.getCreatedAt());
    }
}
