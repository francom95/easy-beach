package com.easybeach.ordering.web.dto;

import com.easybeach.ordering.domain.EstadoPedido;
import jakarta.validation.constraints.NotNull;

/** {@code motivo} es obligatorio para cancelar (validado en el service). */
public record TransicionPedidoRequest(@NotNull EstadoPedido estado, String motivo) {
}
