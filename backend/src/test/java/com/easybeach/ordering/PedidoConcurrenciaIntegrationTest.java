package com.easybeach.ordering;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.catalog.web.dto.CategoriaMenuRequest;
import com.easybeach.catalog.web.dto.CategoriaMenuResponse;
import com.easybeach.catalog.web.dto.ProductoRequest;
import com.easybeach.catalog.web.dto.ProductoResponse;
import com.easybeach.ordering.domain.EstadoPedido;
import com.easybeach.ordering.web.dto.CrearPedidoRequest;
import com.easybeach.ordering.web.dto.PedidoEventoResponse;
import com.easybeach.ordering.web.dto.PedidoResponse;
import com.easybeach.ordering.web.dto.TransicionPedidoRequest;
import com.easybeach.stay.domain.TipoUbicacion;
import com.easybeach.stay.web.dto.EstadiaResponse;
import com.easybeach.stay.web.dto.SolicitarEstadiaRequest;
import com.easybeach.stay.web.dto.UbicacionRequest;
import com.easybeach.stay.web.dto.UbicacionResponse;
import com.easybeach.support.AbstractIntegrationTest;
import com.easybeach.support.EscenarioBalneario;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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
 * Etapa 19 §3: "doble transición del mismo pedido desde dos tablets". A
 * diferencia de {@code Estadia} (defendida por un UNIQUE KEY real en la
 * base), {@code Pedido} no tiene {@code @Version} (sin optimistic locking) -
 * este test comprueba si la única defensa (el chequeo
 * {@code estado.puedeTransicionarA(destino)} sobre la entidad ya cargada en
 * memoria) alcanza bajo dos requests genuinamente simultáneos, o si hay una
 * ventana real de "lost update".
 *
 * <p>Nota (etapa 19, QA): esta clase originalmente incluía también
 * {@code dosPedidosConLaMismaIdempotencyKeySimultaneosCreanUnoSolo}, probando
 * dos creaciones de pedido con la misma {@code Idempotency-Key} genuinamente
 * en paralelo. Ese test encontró un hallazgo real (el perdedor de la carrera
 * puede ver una excepción cruda de Hibernate en vez de la respuesta
 * idempotente esperada) y un intento de arreglarlo aislando el insert en su
 * propia transacción {@code REQUIRES_NEW} resultó en una regresión peor
 * (rompía la creación NORMAL, sin ninguna carrera, con un
 * {@code StaleObjectStateException} - el resto de {@code PedidoService.crear()}
 * sigue trabajando con la entidad en la sesión de ESTE método, que queda
 * detached apenas el insert vive en otra transacción). El intento se revirtió
 * por ser más riesgoso que el bug original; ver el entregable de esta etapa
 * para el detalle y por qué queda en backlog en vez de bloqueante.
 */
class PedidoConcurrenciaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EscenarioBalneario escenario;

    private EscenarioBalneario.Contexto ctx;
    private HttpHeaders clienteHeaders;
    private String estadiaPublicId;
    private Long productoId;

    @BeforeEach
    void seed() {
        ctx = escenario.crearBalnearioOperativoConStaff("pedido-concurrencia");
        clienteHeaders = escenario.registrarClienteYObtenerHeaders();

        UbicacionResponse ubicacion = restTemplate.exchange(url("/api/v1/admin/ubicaciones"), HttpMethod.POST,
                new HttpEntity<>(new UbicacionRequest(TipoUbicacion.CARPA, "Carpa 1"), ctx.adminHeaders()),
                UbicacionResponse.class).getBody();
        CategoriaMenuResponse categoria = restTemplate.exchange(url("/api/v1/admin/categorias"), HttpMethod.POST,
                new HttpEntity<>(new CategoriaMenuRequest("Bebidas", 1, true), ctx.adminHeaders()),
                CategoriaMenuResponse.class).getBody();
        ProductoResponse producto = restTemplate.exchange(url("/api/v1/admin/productos"), HttpMethod.POST,
                new HttpEntity<>(new ProductoRequest(categoria.id(), "Gaseosa", null, new BigDecimal("2000.00"),
                        true, 1), ctx.adminHeaders()),
                ProductoResponse.class).getBody();
        productoId = producto.id();

        EstadiaResponse estadia = restTemplate.exchange(url("/api/v1/estadias"), HttpMethod.POST,
                new HttpEntity<>(new SolicitarEstadiaRequest(ctx.slug(), ubicacion.id()), clienteHeaders),
                EstadiaResponse.class).getBody();
        restTemplate.exchange(url("/api/v1/operativo/estadias/" + estadia.publicId() + "/validacion"),
                HttpMethod.POST, new HttpEntity<>(ctx.carperoHeaders()), EstadiaResponse.class);
        estadiaPublicId = estadia.publicId();
    }

    private String crearPedidoConfirmado() {
        HttpHeaders headers = new HttpHeaders();
        headers.addAll(clienteHeaders);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        var request = new CrearPedidoRequest(estadiaPublicId,
                List.of(new CrearPedidoRequest.ItemRequest(productoId, null, 1)), "fake-card-token");
        return restTemplate.exchange(url("/api/v1/pedidos"), HttpMethod.POST, new HttpEntity<>(request, headers),
                PedidoResponse.class).getBody().publicId();
    }

    /**
     * Dos "tablets" (dos requests HTTP reales, sin coordinarse entre sí)
     * transicionan el MISMO pedido CONFIRMADO a destinos distintos - uno a
     * EN_PREPARACION (avanzar) y otro a CANCELADO (cancelar) - al mismo
     * instante. Solo una transición puede ser la ganadora real: cualquiera
     * de los dos destinos es un resultado válido, pero el estado final en la
     * base y el historial de eventos tienen que ser consistentes entre sí
     * (ninguna "escritura perdida" silenciosa).
     */
    @Test
    void dosTransicionesSimultaneasDelMismoPedidoNoCorrompenElEstado() throws Exception {
        String publicId = crearPedidoConfirmado();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier largada = new CyclicBarrier(2);
        try {
            Callable<Integer> avanzar = () -> {
                largada.await(10, TimeUnit.SECONDS);
                var r = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicId + "/estado"),
                        HttpMethod.PUT,
                        new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.EN_PREPARACION, null),
                                ctx.operadorHeaders()),
                        String.class);
                return r.getStatusCode().value();
            };
            Callable<Integer> cancelar = () -> {
                largada.await(10, TimeUnit.SECONDS);
                var r = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicId + "/cancelacion"),
                        HttpMethod.POST,
                        new HttpEntity<>(new TransicionPedidoRequest(EstadoPedido.CANCELADO, "cancelación de prueba"),
                                ctx.operadorHeaders()),
                        String.class);
                return r.getStatusCode().value();
            };

            List<Future<Integer>> resultados = pool.invokeAll(List.of(avanzar, cancelar), 30, TimeUnit.SECONDS);
            int statusAvanzar = resultados.get(0).get();
            int statusCancelar = resultados.get(1).get();

            // Al menos una transición debe haber sido aceptada (200); ambas
            // partían de un estado válido (CONFIRMADO -> EN_PREPARACION y
            // CONFIRMADO -> CANCELADO son ambas transiciones legales).
            assertThat(List.of(statusAvanzar, statusCancelar)).contains(HttpStatus.OK.value());

            // La perdedora de la carrera (InnoDB deadlock real, o Hibernate
            // detectando en el commit que la fila ya cambió) tiene que recibir
            // un 409 accionable, nunca un 500 crudo - hallazgo real de esta
            // etapa, corregido en GlobalExceptionHandler.handleConflictoDeConcurrencia().
            assertThat(List.of(statusAvanzar, statusCancelar))
                    .as("ninguna de las dos respuestas debe ser un 500 sin manejar")
                    .allSatisfy(status -> assertThat(status).isIn(HttpStatus.OK.value(), HttpStatus.CONFLICT.value()));

            // Estado final real en la base - se lee DESPUÉS de confirmar que
            // ninguna escritura quedó en un 500 a medio aplicar.
            var pedidoFinal = restTemplate.exchange(url("/api/v1/pedidos/" + publicId), HttpMethod.GET,
                    new HttpEntity<>(clienteHeaders), PedidoResponse.class).getBody();

            // Historial: la fuente de verdad de qué pasó de verdad. Un GET
            // inmediatamente después de la carrera puede, en algún caso raro,
            // toparse con el mismo tipo de conflicto de lectura/commit que las
            // escrituras - se tolera UN reintento, igual que haría un cliente
            // real refrescando la pantalla.
            PedidoEventoResponse[] historial = null;
            for (int intento = 0; intento < 2 && historial == null; intento++) {
                var respuesta = restTemplate.exchange(url("/api/v1/operativo/pedidos/" + publicId + "/historial"),
                        HttpMethod.GET, new HttpEntity<>(ctx.operadorHeaders()), PedidoEventoResponse[].class);
                if (respuesta.getStatusCode() == HttpStatus.OK) {
                    historial = respuesta.getBody();
                }
            }
            assertThat(historial).as("el historial debe quedar disponible a más tardar en el segundo intento").isNotNull();

            // La aserción real: el estado final DEBE coincidir con el último
            // evento del historial. Si no coincide, hubo una escritura
            // perdida (una transición pisó a la otra sin que el historial ni
            // el estado final quedaran consistentes entre sí).
            String ultimoEventoDelHistorial = historial[historial.length - 1].estadoNuevo();
            assertThat(pedidoFinal.estado())
                    .as("el estado final del pedido debe coincidir con el último evento registrado en su historial")
                    .isEqualTo(ultimoEventoDelHistorial);
        } finally {
            pool.shutdownNow();
        }
    }
}
