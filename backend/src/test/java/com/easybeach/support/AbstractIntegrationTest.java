package com.easybeach.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;

/**
 * Base para tests de integración: levanta un MySQL real (Testcontainers,
 * criterio de aceptación de la etapa 09 - "docker compose up... tests de
 * integración") y aplica las migraciones Flyway reales contra él.
 *
 * <p><b>Patrón singleton de Testcontainers</b> (recomendado por la propia
 * documentación del proyecto): el contenedor se arranca a mano en un bloque
 * estático, SIN {@code @Container}/{@code @Testcontainers}. Si se usa
 * {@code @Container}, la extensión de JUnit para el contenedor y lo detiene
 * al terminar CADA clase de test - como el campo {@code static} se comparte
 * entre todas las subclases (una sola variable de JVM), la segunda clase de
 * test que corre se queda con un contenedor ya parado ("Communications link
 * failure"). Arrancándolo a mano una única vez, vive durante toda la
 * ejecución de Maven y todas las clases de test lo reutilizan.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FakeMercadoPagoOAuthClient.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL;

    static {
        MYSQL = new MySQLContainer<>("mysql:8.0");
        MYSQL.start();
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    protected int port;

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }
}
