package com.easybeach.promotions.service;

import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.shared.time.ZonaNegocio;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import org.springframework.stereotype.Component;

/**
 * Resuelve si una promoción está vigente "ahora" (etapa 14 criterio de
 * aceptación: bordes de vigencia horaria y de fechas, con la TZ correcta).
 *
 * <p>La TZ de negocio es {@code America/Argentina/Buenos_Aires} (etapa 02
 * §—, etapa 04 §1), no UTC: un happy hour "18:00 a 20:00" se evalúa contra
 * la hora de la playa, no la del servidor.
 */
@Component
public class VigenciaPromocionChecker {

    public boolean estaVigenteAhora(Promocion promocion) {
        return estaVigenteEn(promocion, ZonedDateTime.now(ZonaNegocio.ZONE_ID));
    }

    /** Paquete-visible para tests: permite fijar el instante evaluado. */
    boolean estaVigenteEn(Promocion promocion, ZonedDateTime ahora) {
        if (promocion.getEstado() != EstadoPromocion.ACTIVA) {
            return false;
        }
        LocalDate hoy = ahora.toLocalDate();
        if (promocion.getVigenciaDesde() != null && hoy.isBefore(promocion.getVigenciaDesde())) {
            return false;
        }
        if (promocion.getVigenciaHasta() != null && hoy.isAfter(promocion.getVigenciaHasta())) {
            return false;
        }
        // Día/franja horaria: solo aplican a HAPPY_HOUR (etapa 03 §3.9).
        if (promocion.getTipo() == TipoPromocion.HAPPY_HOUR) {
            if (!diaPermitido(promocion.getDiasSemana(), ahora.getDayOfWeek())) {
                return false;
            }
            if (!enFranjaHoraria(promocion.getFranjaHoraDesde(), promocion.getFranjaHoraHasta(),
                    ahora.toLocalTime())) {
                return false;
            }
        }
        return true;
    }

    private boolean diaPermitido(String diasSemana, DayOfWeek diaActual) {
        if (diasSemana == null || diasSemana.isBlank()) {
            return true; // sin restricción: todos los días
        }
        String codigo = codigoDe(diaActual);
        return Arrays.stream(diasSemana.split(","))
                .map(String::trim)
                .anyMatch(codigo::equalsIgnoreCase);
    }

    private String codigoDe(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "LUN";
            case TUESDAY -> "MAR";
            case WEDNESDAY -> "MIE";
            case THURSDAY -> "JUE";
            case FRIDAY -> "VIE";
            case SATURDAY -> "SAB";
            case SUNDAY -> "DOM";
        };
    }

    private boolean enFranjaHoraria(LocalTime desde, LocalTime hasta, LocalTime ahora) {
        if (desde == null || hasta == null) {
            return true; // sin restricción horaria
        }
        if (desde.equals(hasta)) {
            return true; // franja de 24hs
        }
        if (desde.isBefore(hasta)) {
            // Franja normal dentro del mismo día: [desde, hasta).
            return !ahora.isBefore(desde) && ahora.isBefore(hasta);
        }
        // Franja que cruza medianoche (ej. 20:00 a 02:00): [desde, 24:00) U [00:00, hasta).
        return !ahora.isBefore(desde) || ahora.isBefore(hasta);
    }
}
