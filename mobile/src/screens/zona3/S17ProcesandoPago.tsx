import React, {useEffect, useState} from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {EstadoCargando} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {obtenerPedido} from '../../api/pedidos';
import type {RootScreenProps} from '../../navigation/types';

const POLL_MS = 3000;
const SEGUNDOS_PARA_CAMBIAR_MENSAJE = 30;

/**
 * Bloqueante a propósito (etapa 07): sin botón de volver, evita pagos
 * duplicados por doble tap. El pedido pasa a CONFIRMADO recién cuando el
 * webhook de MP lo aprueba (etapa 13) - acá se hace polling (fallback de
 * ADR-003) porque el cliente SSE todavía no está conectado en este punto
 * del flujo (se conecta desde MainTabs).
 */
export function S17ProcesandoPago({route, navigation}: RootScreenProps<'S17ProcesandoPago'>) {
  const {pedidoPublicId} = route.params;
  const {tokens, spacing} = useTheme();
  const [segundos, setSegundos] = useState(0);

  const pedidoQuery = useQuery({
    queryKey: ['pedido', pedidoPublicId],
    queryFn: () => obtenerPedido(pedidoPublicId),
    refetchInterval: POLL_MS,
  });

  useEffect(() => {
    const intervalo = setInterval(() => setSegundos(s => s + 1), 1000);
    return () => clearInterval(intervalo);
  }, []);

  useEffect(() => {
    if (pedidoQuery.data?.estado === 'CONFIRMADO') {
      navigation.replace('S18PagoAprobado', {pedidoPublicId});
    } else if (pedidoQuery.data?.estado === 'PAGO_RECHAZADO') {
      navigation.replace('S19PagoRechazado', {pedidoPublicId, motivoDetalle: 'El pago no fue aprobado'});
    }
  }, [pedidoQuery.data?.estado, navigation, pedidoPublicId]);

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md}}>
        <EstadoCargando
          texto={
            segundos > SEGUNDOS_PARA_CAMBIAR_MENSAJE
              ? 'Está tardando más de lo normal - te avisamos apenas se confirme'
              : 'Procesando tu pago...'
          }
        />
        <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>No cierres la app</Text>
      </View>
    </ScreenContainer>
  );
}
