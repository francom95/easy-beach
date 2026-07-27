package com.easybeach.reporting.dto;

import java.time.LocalDate;

public record AperturasPorDiaResponse(LocalDate dia, long cantidad) {
}
