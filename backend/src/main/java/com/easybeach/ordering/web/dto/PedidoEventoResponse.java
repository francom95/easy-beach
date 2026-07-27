package com.easybeach.ordering.web.dto;

import java.time.Instant;

public record PedidoEventoResponse(String estadoAnterior, String estadoNuevo,
                                    String actorTipo, String motivo, Instant momento) {
}
