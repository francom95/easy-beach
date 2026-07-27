package com.easybeach.reporting.dto;

import java.util.List;

/**
 * Reporte de Super Admin (etapa 15): el único que cruza datos entre
 * balnearios, por diseño. {@code volumenPorBalneario} se acota a la
 * temporada {@code EN_CURSO}.
 */
public record PlataformaReporteResponse(long balneariosActivos, List<VolumenPorBalnearioResponse> volumenPorBalneario) {
}
