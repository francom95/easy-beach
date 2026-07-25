package com.easybeach;

import com.easybeach.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class EasyBeachApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Si el contexto no levanta (migraciones Flyway, seguridad, JPA), este test falla.
    }
}
