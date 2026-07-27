package com.easybeach.stay;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Etapa 12 criterio de aceptación: "el constraint de unicidad está en la base
 * de datos y hay un test de concurrencia que lo demuestra".
 *
 * <p>La validación previa en el service NO es la defensa (dos requests
 * simultáneos pueden leer "no hay estadía" a la vez); la garantía real es el
 * UK {@code (balneario_id, activa_uk)}. Este test dispara N solicitudes
 * genuinamente en paralelo para provocar esa carrera.
 */
class EstadiaConcurrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final int INTENTOS_SIMULTANEOS = 8;

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private Long ubicacionId;
    private HttpHeaders clienteHeaders;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("concurrencia");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();
        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        ubicacionId = ubicacion.id();
    }

    @Test
    void ochoSolicitudesSimultaneasDelMismoClienteCreanUnaSolaEstadia() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(INTENTOS_SIMULTANEOS);
        // La barrera hace que los 8 threads salgan en el mismo instante: sin
        // esto, se serializan solos y la carrera nunca ocurre.
        CyclicBarrier largada = new CyclicBarrier(INTENTOS_SIMULTANEOS);

        try {
            List<Callable<Integer>> intentos = java.util.stream.IntStream.range(0, INTENTOS_SIMULTANEOS)
                    .<Callable<Integer>>mapToObj(i -> () -> {
                        largada.await(10, TimeUnit.SECONDS);
                        var response = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacionId), clienteHeaders),
                                String.class);
                        return response.getStatusCode().value();
                    })
                    .toList();

            List<Future<Integer>> resultados = pool.invokeAll(intentos, 30, TimeUnit.SECONDS);

            long creadas = 0;
            long conflictos = 0;
            for (Future<Integer> f : resultados) {
                int status = f.get();
                if (status == HttpStatus.CREATED.value()) {
                    creadas++;
                } else if (status == HttpStatus.CONFLICT.value()) {
                    conflictos++;
                }
            }

            // La regla de negocio no depende de la suerte del scheduler:
            // exactamente una gana, todas las demás reciben 409.
            assertThat(creadas).as("solicitudes creadas").isEqualTo(1);
            assertThat(conflictos).as("rechazadas por conflicto").isEqualTo(INTENTOS_SIMULTANEOS - 1);

            EstadiaResponse[] vigentes = restTemplate.exchange(url("/api/v1/estadias/vigentes"), HttpMethod.GET,
                    new HttpEntity<>(clienteHeaders), EstadiaResponse[].class).getBody();
            assertThat(vigentes).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
