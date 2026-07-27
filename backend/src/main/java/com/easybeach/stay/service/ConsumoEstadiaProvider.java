package com.easybeach.stay.service;

import java.math.BigDecimal;

/**
 * <b>Inversión de dependencia (ADR-002).</b> El cierre de estadía necesita
 * dos datos que pertenecen a {@code ordering}: si quedan pedidos en curso
 * (para bloquear el cierre) y el consumo acumulado (para el resumen). Pero
 * la flecha de dependencia va {@code ordering -> stay}, nunca al revés.
 *
 * <p>La interfaz vive acá (en el consumidor) y la implementación real la
 * aporta {@code ordering} en la etapa 13. Hasta entonces rige
 * {@link SinPedidosConsumoProvider}, que responde "no hay pedidos" - correcto
 * por construcción mientras el módulo de pedidos no existe.
 *
 * <p>No se resuelve con un evento (como {@code BalnearioCreado} en la etapa
 * 10) porque el cierre necesita la respuesta de forma <b>síncrona</b> para
 * decidir si sigue adelante o falla.
 */
public interface ConsumoEstadiaProvider {

    /** Etapa 12 (decisión de negocio): con pedidos en curso, el cierre se BLOQUEA. */
    boolean tienePedidosEnCurso(Long estadiaId);

    ResumenConsumo obtenerResumen(Long estadiaId);

    /** Cantidad y monto de los pedidos ENTREGADOS de la estadía. */
    record ResumenConsumo(int cantidadPedidos, BigDecimal montoTotal) {

        public static ResumenConsumo vacio() {
            return new ResumenConsumo(0, BigDecimal.ZERO);
        }
    }
}
