package com.easybeach.branding.theming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Set curado de 4 parejas tipográficas embebidas en el binario (tokens.md). */
public enum TypographyFamily {
    CLARA,
    AMIGABLE,
    ELEGANTE,
    ENERGICA;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static TypographyFamily fromJson(String value) {
        return TypographyFamily.valueOf(value.toUpperCase());
    }
}
