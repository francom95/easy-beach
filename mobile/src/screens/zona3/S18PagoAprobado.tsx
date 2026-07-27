import React from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoCargando} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {obtenerPedido} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

/** Fin del camino de 4 taps (etapa 07): 3 certezas juntas - se cobró, está en el seguimiento, va camino a tu ubicación. */
export function S18PagoAprobado({route, navigation}: RootScreenProps<'S18PagoAprobado'>) {
  const {pedidoPublicId} = route.params;
  const {tokens, spacing, typeScale} = useTheme();
  const pedidoQuery = useQuery({queryKey: ['pedido', pedidoPublicId], queryFn: () => obtenerPedido(pedidoPublicId)});

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="exito" texto="¡Pago aprobado!" tamano={40} />
        {pedidoQuery.isLoading ? (
          <EstadoCargando />
        ) : (
          <>
            <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>
              Tu pedido está confirmado. Te avisamos cuando esté en camino.
            </Text>
            <Text style={{fontSize: typeScale.price, fontWeight: '800', color: tokens['color.on-surface']}}>
              {formatearMonto(pedidoQuery.data?.total ?? '0')}
            </Text>
          </>
        )}
        <View style={{width: '100%', marginTop: spacing.xl}}>
          <Button titulo="Ver seguimiento del pedido" onPress={() => navigation.replace('S20DetallePedido', {pedidoPublicId})} />
          <View style={{marginTop: spacing.md}}>
            <Button
              titulo="Volver al inicio"
              variante="contorno"
              onPress={() => navigation.reset({index: 0, routes: [{name: 'MainTabs'}]})}
            />
          </View>
        </View>
      </View>
    </ScreenContainer>
  );
}
