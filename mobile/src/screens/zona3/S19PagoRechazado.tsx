import React from 'react';
import {Text, View} from 'react-native';
import {useMutation} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {usePaymentStore} from '../../store/paymentStore';
import {crearPedido} from '../../api/pedidos';
import {generarIdempotencyKey} from '../../utils/idempotency';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Lidera con "no se te cobró nada" (el miedo real, etapa 07). El carrito
 * sigue intacto en cartStore - este pedido rechazado no lo vació (a
 * diferencia del exitoso, ver S15). 2 reintentos de un tap: misma tarjeta u
 * otra. Cada reintento usa una clave de idempotencia NUEVA (es un intento de
 * pago distinto, no un reenvío del mismo).
 */
export function S19PagoRechazado({route, navigation}: RootScreenProps<'S19PagoRechazado'>) {
  const {motivoDetalle} = route.params;
  const {tokens, spacing} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const cart = useCartStore();
  const {cardTokenGuardado} = usePaymentStore();

  const reintentarMutation = useMutation({
    mutationFn: () =>
      crearPedido(
        estadia!.publicId,
        cart.items.map(i => ({productoId: i.productoId, productoVarianteId: i.productoVarianteId, cantidad: i.cantidad})),
        cardTokenGuardado!,
        generarIdempotencyKey(),
      ),
    onSuccess: pedido => {
      if (pedido.estado === 'CONFIRMADO') {
        cart.vaciar();
        navigation.replace('S18PagoAprobado', {pedidoPublicId: pedido.publicId});
      } else {
        navigation.replace('S17ProcesandoPago', {pedidoPublicId: pedido.publicId});
      }
    },
  });

  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="error" texto="No se te cobró nada" tamano={32} />
        <Text style={{color: tokens['color.on-surface-muted']}}>{motivoDetalle}</Text>

        <Button
          titulo="Reintentar con la misma tarjeta"
          onPress={() => reintentarMutation.mutate()}
          cargando={reintentarMutation.isPending}
        />
        <Button
          titulo="Usar otra tarjeta"
          onPress={() => navigation.navigate('S16HojaMedioPago')}
          variante="contorno"
        />

        {reintentarMutation.isError ? <EstadoError mensaje={(reintentarMutation.error as ApiError).detail} /> : null}
      </View>
    </ScreenContainer>
  );
}
