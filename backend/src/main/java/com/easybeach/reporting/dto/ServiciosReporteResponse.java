package com.easybeach.reporting.dto;

import java.util.List;

/** {@code tiempoResolucionPromedioMinutos}: solo solicitudes RESUELTA (proxy: created_at -> updated_at). */
public record ServiciosReporteResponse(List<SolicitudesPorTipoResponse> porTipo,
                                        Double tiempoResolucionPromedioMinutos) {
}
