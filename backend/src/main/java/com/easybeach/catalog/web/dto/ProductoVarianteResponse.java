package com.easybeach.catalog.web.dto;

import java.math.BigDecimal;

public record ProductoVarianteResponse(Long id, String nombre, BigDecimal precio, boolean disponible, int orden) {
}
