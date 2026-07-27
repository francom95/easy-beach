import React, {useState} from 'react';
import {FlatList, Image, Pressable, StyleSheet, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {obtenerMenu} from '../../api/catalogo';
import {formatearMonto} from '../../utils/money';
import type {MenuProductoResponse} from '../../api/types';
import type {TabScreenProps} from '../../navigation/types';

/**
 * 2 columnas, fotos grandes (etapa 07). El "+" se vuelve stepper en el
 * lugar sin navegar cuando el producto no tiene variantes; con variantes,
 * abre S13 (el precio se congela recién ahí). Barra de carrito pegajosa
 * arriba de la tab bar con cantidad+total.
 */
export function S11Menu({navigation}: TabScreenProps<'S11Menu'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const cartStore = useCartStore();
  const [categoriaActiva, setCategoriaActiva] = useState<number | null>(null);

  const menuQuery = useQuery({queryKey: ['menu', balnearioSlug], queryFn: () => obtenerMenu(balnearioSlug)});

  const categorias = menuQuery.data ?? [];
  const categoria = categorias.find(c => c.id === categoriaActiva) ?? categorias[0];

  function alTocarProducto(producto: MenuProductoResponse) {
    if (producto.variantes.length > 0) {
      navigation.navigate('S13DetalleConVariantes', {productoId: producto.id});
      return;
    }
    cartStore.agregar(producto, null);
  }

  if (menuQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando texto="Cargando el menú..." />
      </ScreenContainer>
    );
  }
  if (menuQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar el menú." onReintentar={() => menuQuery.refetch()} />
      </ScreenContainer>
    );
  }
  if (categorias.length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio titulo="El menú todavía no tiene productos" />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer style={{padding: 0}}>
      <FlatList
        horizontal
        showsHorizontalScrollIndicator={false}
        data={categorias}
        keyExtractor={c => String(c.id)}
        contentContainerStyle={{padding: spacing.lg, gap: spacing.sm}}
        renderItem={({item}) => {
          const activa = item.id === (categoria?.id ?? categorias[0].id);
          return (
            <Pressable
              onPress={() => setCategoriaActiva(item.id)}
              style={[
                styles.tabCategoria,
                {
                  backgroundColor: activa ? tokens['color.primary'] : tokens['color.surface'],
                  borderRadius: radii.pill,
                  borderColor: tokens['color.border'],
                },
              ]}>
              <Text style={{color: activa ? tokens['color.on-primary'] : tokens['color.on-surface'], fontWeight: '600'}}>
                {item.nombre}
              </Text>
            </Pressable>
          );
        }}
      />

      <FlatList
        data={categoria?.productos ?? []}
        keyExtractor={p => String(p.id)}
        numColumns={2}
        contentContainerStyle={{padding: spacing.lg, gap: spacing.md, paddingBottom: 100}}
        columnWrapperStyle={{gap: spacing.md}}
        ListEmptyComponent={<EstadoVacio titulo="Sin productos en esta categoría" />}
        renderItem={({item}) => {
          const itemCarrito = cartStore.items.find(i => i.productoId === item.id && !i.productoVarianteId);
          return (
            <Pressable
              onPress={() => (item.variantes.length > 0 ? navigation.navigate('S13DetalleConVariantes', {productoId: item.id}) : navigation.navigate('S12DetalleProductoSimple', {productoId: item.id}))}
              style={[
                styles.tarjetaProducto,
                {backgroundColor: tokens['color.surface'], borderRadius: radii.card, borderColor: tokens['color.border']},
              ]}>
              {item.fotoUrl ? (
                <Image source={{uri: item.fotoUrl}} style={styles.foto} resizeMode="cover" />
              ) : (
                <View style={[styles.foto, {backgroundColor: tokens['color.background']}]} />
              )}
              {item.promociones.length > 0 ? (
                <View style={[styles.badgePromo, {backgroundColor: tokens['color.warning'], borderRadius: radii.pill}]}>
                  <Text style={{color: tokens['color.on-warning'], fontSize: 11, fontWeight: '700'}}>PROMO</Text>
                </View>
              ) : null}
              <View style={{padding: spacing.sm}}>
                <Text numberOfLines={1} style={{color: tokens['color.on-surface'], fontWeight: '700'}}>
                  {item.nombre}
                </Text>
                <Text style={{color: tokens['color.on-surface'], fontSize: typeScale.price, fontWeight: '800', marginTop: 2}}>
                  {formatearMonto(item.precioBase)}
                </Text>
                {item.variantes.length === 0 ? (
                  <Pressable
                    onPress={() => alTocarProducto(item)}
                    style={[styles.botonMas, {backgroundColor: tokens['color.primary'], borderRadius: radii.pill}]}>
                    <Text style={{color: tokens['color.on-primary'], fontWeight: '800'}}>
                      {itemCarrito ? itemCarrito.cantidad : '+'}
                    </Text>
                  </Pressable>
                ) : null}
              </View>
            </Pressable>
          );
        }}
      />

      {cartStore.cantidadTotal() > 0 ? (
        <Pressable
          onPress={() => navigation.navigate('S15Carrito')}
          style={[styles.barraCarrito, {backgroundColor: tokens['color.primary']}]}>
          <Text style={{color: tokens['color.on-primary'], fontWeight: '700'}}>
            Ver carrito ({cartStore.cantidadTotal()})
          </Text>
          <Text style={{color: tokens['color.on-primary'], fontWeight: '700'}}>
            {formatearMonto(cartStore.subtotal())}
          </Text>
        </Pressable>
      ) : null}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  tabCategoria: {paddingVertical: 8, paddingHorizontal: 16, borderWidth: StyleSheet.hairlineWidth},
  tarjetaProducto: {flex: 1, borderWidth: StyleSheet.hairlineWidth, overflow: 'hidden'},
  foto: {width: '100%', aspectRatio: 1.3},
  badgePromo: {position: 'absolute', top: 8, left: 8, paddingHorizontal: 8, paddingVertical: 2},
  botonMas: {
    position: 'absolute',
    right: 8,
    bottom: 8,
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  barraCarrito: {
    position: 'absolute',
    bottom: 12,
    left: 16,
    right: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderRadius: 999,
  },
});
