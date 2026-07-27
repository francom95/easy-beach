package com.easybeach.reporting.service;

import com.easybeach.shared.error.ApiException;
import com.easybeach.shared.error.ErrorCode;
import com.easybeach.shared.time.ZonaNegocio;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Convierte un rango de fechas "de negocio" (etapa 04: filtros de fecha
 * respetan la TZ de negocio, no UTC) a los límites {@link Instant} que las
 * columnas {@code created_at}/{@code fecha_*} (siempre UTC, etapa 03 §1)
 * necesitan para el {@code WHERE}.
 *
 * <p>{@code hasta} es <b>inclusivo</b> desde la perspectiva del usuario del
 * reporte ("del 1 al 31 de enero"), así que el límite superior real usado
 * en la query es el inicio del día siguiente (exclusivo).
 */
final class RangoFechasUtil {

    private RangoFechasUtil() {
    }

    record Rango(Instant desde, Instant hasta) {
    }

    static Rango resolver(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new ApiException(ErrorCode.VALIDACION_FALLIDA, "desde no puede ser posterior a hasta");
        }
        Instant inicio = desde.atStartOfDay(ZonaNegocio.ZONE_ID).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(ZonaNegocio.ZONE_ID).toInstant();
        return new Rango(inicio, fin);
    }
}
