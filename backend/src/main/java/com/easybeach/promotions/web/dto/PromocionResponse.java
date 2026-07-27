package com.easybeach.promotions.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PromocionResponse(
        Long id,
        String nombre,
        String tipo,
        String estado,
        BigDecimal valor,
        LocalDate vigenciaDesde,
        LocalDate vigenciaHasta,
        LocalTime franjaHoraDesde,
        LocalTime franjaHoraHasta,
        String diasSemana,
        List<AlcancePromocionRequest> alcances,
        List<ComboItemRequest> comboItems
) {
}
