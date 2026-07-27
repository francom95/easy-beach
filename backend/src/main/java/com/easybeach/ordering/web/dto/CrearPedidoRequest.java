package com.easybeach.ordering.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * El carrito vive en el cliente; acá llega el pedido completo. <b>No hay
 * campo de precio ni de total</b>: los calcula el servidor (etapa 03 §3.6).
 * Si el cliente los mandara, se ignorarían - directamente no existen en el
 * contrato.
 */
public record CrearPedidoRequest(
        @NotBlank String estadiaPublicId,
        @NotEmpty @Valid List<ItemRequest> items,
        /** Token de tarjeta generado por el SDK de MP en el dispositivo (etapa 05 §4.3). */
        String cardToken
) {

    public record ItemRequest(
            @NotNull Long productoId,
            Long productoVarianteId,
            @Positive int cantidad
    ) {
    }
}
