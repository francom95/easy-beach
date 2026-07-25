package com.easybeach.platform.web.dto;

import jakarta.validation.constraints.NotNull;

public record SuscribirRequest(@NotNull Long planId, @NotNull Long temporadaId) {
}
