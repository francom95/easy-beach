package com.easybeach.stay.web.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarUbicacionRequest(@NotNull Long ubicacionId) {
}
