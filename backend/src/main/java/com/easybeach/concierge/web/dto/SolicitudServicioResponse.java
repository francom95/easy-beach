package com.easybeach.concierge.web.dto;

import java.time.Instant;

public record SolicitudServicioResponse(String publicId, String tipoServicioNombre, String ubicacionIdentificador,
                                         String nota, String estado, Instant createdAt) {
}
