import React from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoCargando} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {obtenerPedido} from '../../api/pedidos';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Orden deliberado (etapa 07): motivo -> reembolso -> alternativa concreta.
 * El momento de mayor riesgo de pérdida de cliente - se responde con un
 * reemplazo, no solo una disculpa.
 */
export function S22PedidoCancelado({route, navigation}: RootScreenProps<'S22PedidoCancelado'>) {
  const {pedidoPublicId} = route.params;
  const {tokens, spacing} = useTheme();
  const pedidoQuery = useQuery({queryKey: ['pedido', pedidoPublicId], queryFn: () => obtenerPedido(pedidoPublicId)});

  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="advertencia" texto="El local canceló tu pedido" tamano={32} />
        {pedidoQuery.isLoading ? (
          <EstadoCargando />
        ) : (
          <Text style={{color: tokens['color.on-surface-muted']}}>
            {pedidoQuery.data?.motivoCancelacion ?? 'No se especificó un motivo.'}
          </Text>
        )}
        <Text style={{color: tokens['color.on-surface-muted']}}>
          Si ya habías pagado, el reembolso se procesa automáticamente a tu medio de pago original.
        </Text>
        <Button
          titulo="Elegir otro producto"
          onPress={() => navigation.reset({index: 0, routes: [{name: 'MainTabs'}]})}
        />
      </View>
    </ScreenContainer>
  );
}
