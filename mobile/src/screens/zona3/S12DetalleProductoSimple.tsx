import React, {useState} from 'react';
import {Image, ScrollView, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoCargando, EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {obtenerMenu} from '../../api/catalogo';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

/** Se llega tocando foto/nombre (no el "+"): resuelve dudas - descripción, cantidad. El CTA repite el total. */
export function S12DetalleProductoSimple({route, navigation}: RootScreenProps<'S12DetalleProductoSimple'>) {
  const {productoId} = route.params;
  const {tokens, spacing, typeScale, radii} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const agregar = useCartStore(s => s.agregar);
  const [cantidad, setCantidad] = useState(1);

  const menuQuery = useQuery({queryKey: ['menu', balnearioSlug], queryFn: () => obtenerMenu(balnearioSlug)});
  const producto = menuQuery.data?.flatMap(c => c.productos).find(p => p.id === productoId);

  if (menuQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }
  if (menuQuery.isError || !producto) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar este producto." onReintentar={() => menuQuery.refetch()} />
      </ScreenContainer>
    );
  }
  const total = Number(producto.precioBase) * cantidad;

  return (
    <ScreenContainer>
      <ScrollView style={{flex: 1}}>
        {producto.fotoUrl ? (
          <Image source={{uri: producto.fotoUrl}} style={{width: '100%', aspectRatio: 1.4, borderRadius: radii.card, marginBottom: spacing.md}} />
        ) : null}
        <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background']}}>
          {producto.nombre}
        </Text>
        {producto.descripcion ? (
          <Text style={{color: tokens['color.on-surface-muted'], marginTop: spacing.sm}}>{producto.descripcion}</Text>
        ) : null}

        <View style={{flexDirection: 'row', alignItems: 'center', gap: spacing.md, marginVertical: spacing.lg}}>
          <Button titulo="-" onPress={() => setCantidad(c => Math.max(1, c - 1))} fullWidth={false} variante="contorno" />
          <Text style={{fontSize: typeScale.title, fontWeight: '700'}}>{cantidad}</Text>
          <Button titulo="+" onPress={() => setCantidad(c => c + 1)} fullWidth={false} variante="contorno" />
        </View>
      </ScrollView>

      <Button
        titulo={`Agregar · ${formatearMonto(total)}`}
        onPress={() => {
          for (let i = 0; i < cantidad; i++) {
            agregar(producto, null);
          }
          navigation.goBack();
        }}
      />
    </ScreenContainer>
  );
}
