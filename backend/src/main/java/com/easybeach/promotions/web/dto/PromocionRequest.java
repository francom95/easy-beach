package com.easybeach.promotions.web.dto;

import com.easybeach.promotions.domain.TipoPromocion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Un único DTO para los tres tipos de promoción (etapa 14): según
 * {@code tipo}, el service exige {@code alcances} (descuento%/happy hour) o
 * {@code comboItems} (combo). {@code diasSemana}: códigos separados por coma
 * ("LUN,MAR,...", ver {@code VigenciaPromocionChecker}), solo relevante para
 * {@code HAPPY_HOUR}.
 */
public record PromocionRequest(
        @NotBlank String nombre,
        @NotNull TipoPromocion tipo,
        boolean activa,
        @NotNull @PositiveOrZero BigDecimal valor,
        LocalDate vigenciaDesde,
        LocalDate vigenciaHasta,
        LocalTime franjaHoraDesde,
        LocalTime franjaHoraHasta,
        String diasSemana,
        List<AlcancePromocionRequest> alcances,
        List<ComboItemRequest> comboItems
) {
}
