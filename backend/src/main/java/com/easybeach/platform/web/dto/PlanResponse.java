package com.easybeach.platform.web.dto;

import java.math.BigDecimal;

public record PlanResponse(Long id, String nombre, String descripcion, BigDecimal precio, boolean activo) {
}
