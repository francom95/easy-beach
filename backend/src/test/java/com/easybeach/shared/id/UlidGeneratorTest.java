package com.easybeach.shared.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UlidGeneratorTest {

    @Test
    void generaVeintiseisCaracteresCrockfordBase32() {
        String ulid = UlidGenerator.generate();
        assertThat(ulid).hasSize(26);
        assertThat(ulid).matches("[0-9A-HJKMNP-TV-Z]{26}");
    }

    @Test
    void esUnicoEnGeneracionesRepetidas() {
        Set<String> generados = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            assertThat(generados.add(UlidGenerator.generate())).isTrue();
        }
    }
}
