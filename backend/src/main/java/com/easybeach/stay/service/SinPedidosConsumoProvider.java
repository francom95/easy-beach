package com.easybeach.stay.service;

/**
 * Implementación vigente hasta la etapa 13: sin módulo {@code ordering}, no
 * puede haber pedidos, así que "ninguno en curso" y "consumo cero" es la
 * respuesta correcta - no un stub que miente.
 *
 * <p>Se registra con {@code @ConditionalOnMissingBean} (ver
 * {@link StayConsumoConfig}): en cuanto {@code ordering} publique su propia
 * implementación, esta desaparece sin tocar una línea de {@code stay}.
 */
public class SinPedidosConsumoProvider implements ConsumoEstadiaProvider {

    @Override
    public boolean tienePedidosEnCurso(Long estadiaId) {
        return false;
    }

    @Override
    public ResumenConsumo obtenerResumen(Long estadiaId) {
        return ResumenConsumo.vacio();
    }
}
