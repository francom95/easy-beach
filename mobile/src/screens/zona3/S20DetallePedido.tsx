import React from 'react';
import {ScrollView, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {Card} from '../../components/Card';
import {EstadoCargando, EstadoError} from '../../components/EstadoCarga';
import {StateRow, type TipoEstado} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useCartStore} from '../../store/cartStore';
import {obtenerPedido, historialDePedido} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {EstadoPedido} from '../../api/types';
import type {RootScreenProps} from '../../navigation/types';

const ETIQUETA_ESTADO: Record<EstadoPedido, string> = {
  CREADO: 'Creado',
  PAGO_PENDIENTE: 'Procesando pago',
  PAGO_RECHAZADO: 'Pago rechazado',
  CONFIRMADO: 'Confirmado',
  EN_PREPARACION: 'En preparación',
  EN_CAMINO: 'En camino',
  ENTREGADO: 'Entregado',
  CANCELADO: 'Cancelado',
};

const TIPO_POR_ESTADO: Record<EstadoPedido, TipoEstado> = {
  CREADO: 'info',
  PAGO_PENDIENTE: 'espera',
  PAGO_RECHAZADO: 'error',
  CONFIRMADO: 'exito',
  EN_PREPARACION: 'espera',
  EN_CAMINO: 'espera',
  ENTREGADO: 'exito',
  CANCELADO: 'error',
};

const POLL_MS = 10000;

/**
 * El estado actual se repite grande arriba (legible al sol) + timeline
 * histórico con horarios abajo. "Pedir lo mismo otra vez" cierra el loop de
 * venta desde el propio seguimiento (etapa 07).
 */
export function S20DetallePedido({route, navigation}: RootScreenProps<'S20DetallePedido'>) {
  const {pedidoPublicId} = route.params;
  const {tokens, spacing, typeScale} = useTheme();
  const agregar = useCartStore(s => s.agregar);

  const pedidoQuery = useQuery({
    queryKey: ['pedido', pedidoPublicId],
    queryFn: () => obtenerPedido(pedidoPublicId),
    refetchInterval: POLL_MS,
  });
  const historialQuery = useQuery({
    queryKey: ['pedido-historial', pedidoPublicId],
    queryFn: () => historialDePedido(pedidoPublicId),
    refetchInterval: POLL_MS,
  });

  if (pedidoQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }
  if (pedidoQuery.isError || !pedidoQuery.data) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar este pedido." onReintentar={() => pedidoQuery.refetch()} />
      </ScreenContainer>
    );
  }

  const pedido = pedidoQuery.data;

  function pedirLoMismo() {
    for (const item of pedido.items) {
      agregar(
        {id: 0, nombre: item.nombreProducto, descripcion: null, precioBase: item.precioUnitario, fotoUrl: null, variantes: [], promociones: []},
        item.nombreVariante ? {id: 0, nombre: item.nombreVariante, precio: item.precioUnitario} : null,
      );
    }
    navigation.navigate('S15Carrito');
  }

  return (
    <ScreenContainer>
      <ScrollView style={{flex: 1}}>
        <View style={{alignItems: 'center', marginVertical: spacing.xl}}>
          <StateRow tipo={TIPO_POR_ESTADO[pedido.estado]} texto={ETIQUETA_ESTADO[pedido.estado]} tamano={36} />
        </View>

        <Card style={{marginBottom: spacing.lg}}>
          {pedido.items.map((item, idx) => (
            <View key={idx} style={{flexDirection: 'row', justifyContent: 'space-between', marginBottom: spacing.xs}}>
              <Text style={{color: tokens['color.on-surface']}}>
                {item.cantidad}x {item.nombreProducto}
                {item.nombreVariante ? ` (${item.nombreVariante})` : ''}
              </Text>
              <Text style={{color: tokens['color.on-surface']}}>{formatearMonto(item.subtotalLinea)}</Text>
            </View>
          ))}
          <View style={{flexDirection: 'row', justifyContent: 'space-between', marginTop: spacing.sm}}>
            <Text style={{fontWeight: '800', color: tokens['color.on-surface']}}>Total</Text>
            <Text style={{fontWeight: '800', color: tokens['color.on-surface']}}>{formatearMonto(pedido.total)}</Text>
          </View>
        </Card>

        <Text style={{fontWeight: '700', color: tokens['color.on-background'], marginBottom: spacing.sm}}>Seguimiento</Text>
        {(historialQuery.data ?? []).map((evento, idx) => (
          <View key={idx} style={{flexDirection: 'row', justifyContent: 'space-between', paddingVertical: spacing.xs}}>
            <Text style={{color: tokens['color.on-surface-muted']}}>{ETIQUETA_ESTADO[evento.estadoNuevo]}</Text>
            <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.label}}>
              {new Date(evento.momento).toLocaleTimeString('es-AR', {hour: '2-digit', minute: '2-digit'})}
            </Text>
          </View>
        ))}
      </ScrollView>

      {pedido.estado === 'ENTREGADO' || pedido.estado === 'CANCELADO' ? (
        <Button titulo="Pedir lo mismo otra vez" onPress={pedirLoMismo} />
      ) : null}
    </ScreenContainer>
  );
}
