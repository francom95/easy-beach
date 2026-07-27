import React from 'react';
import {Linking, Pressable, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {Card} from '../../components/Card';
import {StateRow} from '../../components/StateRow';
import {BannerSinConexion} from '../../components/BannerSinConexion';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {pedidosDeEstadia} from '../../api/pedidos';
import {obtenerBalneario} from '../../api/balnearios';
import {formatearMonto} from '../../utils/money';
import type {TabScreenProps} from '../../navigation/types';

const ESTADOS_EN_CURSO = ['CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO', 'CONFIRMADO', 'EN_PREPARACION', 'EN_CAMINO'];

/**
 * Tap 1 del camino de 4 taps (etapa 07). "Pedí de nuevo" es el atajo de
 * mayor conversión; el pedido en curso vive acá (no obliga a ir a
 * "Pedidos"); el botón de carpero siempre alcanzable con el pulgar.
 */
export function S09Home({navigation}: TabScreenProps<'S09Home'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const balnearioSlug = useStayStore(s => s.balnearioSlug);
  const balnearioNombre = useStayStore(s => s.balnearioNombre);
  const agregarAlCarrito = useCartStore(s => s.agregar);

  const pedidosQuery = useQuery({
    queryKey: ['pedidos', estadia?.publicId],
    queryFn: () => pedidosDeEstadia(estadia!.publicId),
    enabled: Boolean(estadia),
    refetchInterval: 15000,
  });

  const balnearioQuery = useQuery({
    queryKey: ['balneario', balnearioSlug],
    queryFn: () => obtenerBalneario(balnearioSlug!),
    enabled: Boolean(balnearioSlug),
  });

  const pedidoEnCurso = pedidosQuery.data?.find(p => ESTADOS_EN_CURSO.includes(p.estado));
  const ultimoEntregado = pedidosQuery.data?.find(p => p.estado === 'ENTREGADO');

  function pedirDeNuevo() {
    if (!ultimoEntregado) {
      return;
    }
    for (const item of ultimoEntregado.items) {
      agregarAlCarrito(
        {
          id: 0,
          nombre: item.nombreProducto,
          descripcion: null,
          precioBase: item.precioUnitario,
          fotoUrl: null,
          variantes: [],
          promociones: [],
        },
        item.nombreVariante ? {id: 0, nombre: item.nombreVariante, precio: item.precioUnitario} : null,
      );
    }
    navigation.navigate('S15Carrito');
  }

  function llamarACarpero() {
    const telefono = balnearioQuery.data?.telefono;
    if (telefono) {
      Linking.openURL(`tel:${telefono}`);
    }
  }

  return (
    <ScreenContainer scroll>
      <BannerSinConexion />
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginVertical: spacing.md}}>
        {balnearioNombre}
      </Text>
      <Text style={{color: tokens['color.on-surface-muted'], marginBottom: spacing.lg}}>
        {estadia?.ubicacionIdentificador}
      </Text>

      {pedidoEnCurso ? (
        <Pressable onPress={() => navigation.navigate('S20DetallePedido', {pedidoPublicId: pedidoEnCurso.publicId})}>
          <Card style={{marginBottom: spacing.lg}}>
            <StateRow tipo="espera" texto={`Tu pedido está ${pedidoEnCurso.estado.toLowerCase().replace('_', ' ')}`} />
            <Text style={{marginTop: spacing.xs, color: tokens['color.on-surface-muted']}}>
              Total: {formatearMonto(pedidoEnCurso.total)} · Ver seguimiento
            </Text>
          </Card>
        </Pressable>
      ) : null}

      <Button titulo="Pedir del menú" onPress={() => navigation.navigate('S11Menu')} />

      {ultimoEntregado ? (
        <View style={{marginTop: spacing.md}}>
          <Button titulo="Pedí de nuevo" onPress={pedirDeNuevo} variante="secundario" />
        </View>
      ) : null}

      <View style={{marginTop: spacing.xl}}>
        <Button titulo="Pedir sombra / hielo / lo que necesites" onPress={() => navigation.navigate('S23ServiciosHoja')} variante="contorno" />
      </View>
      {balnearioQuery.data?.telefono ? (
        <View style={{marginTop: spacing.md}}>
          <Button titulo="Llamar al carpero" onPress={llamarACarpero} variante="contorno" />
        </View>
      ) : null}
    </ScreenContainer>
  );
}
