import React, {useState} from 'react';
import {Image, Pressable, ScrollView, Text} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoCargando, EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {obtenerMenu} from '../../api/catalogo';
import {formatearMonto} from '../../utils/money';
import type {MenuVarianteResponse} from '../../api/types';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Una sola dimensión por producto (sabor O tamaño, nunca combinadas -
 * decisión de la etapa 01). Lista de radio (filas de 52px); primera opción
 * preseleccionada (mantiene el camino rápido de 1 tap); el precio se
 * congela recién al elegir.
 */
export function S13DetalleConVariantes({route, navigation}: RootScreenProps<'S13DetalleConVariantes'>) {
  const {productoId} = route.params;
  const {tokens, spacing, typeScale, radii} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const agregar = useCartStore(s => s.agregar);

  const menuQuery = useQuery({queryKey: ['menu', balnearioSlug], queryFn: () => obtenerMenu(balnearioSlug)});
  const producto = menuQuery.data?.flatMap(c => c.productos).find(p => p.id === productoId);
  const [varianteElegida, setVarianteElegida] = useState<MenuVarianteResponse | null>(
    producto?.variantes[0] ?? null,
  );

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
  if (producto.variantes.length === 0) {
    navigation.replace('S12DetalleProductoSimple', {productoId});
    return null;
  }

  const elegida = varianteElegida ?? producto.variantes[0];

  return (
    <ScreenContainer>
      <ScrollView style={{flex: 1}}>
        {producto.fotoUrl ? (
          <Image source={{uri: producto.fotoUrl}} style={{width: '100%', aspectRatio: 1.4, borderRadius: radii.card, marginBottom: spacing.md}} />
        ) : null}
        <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.md}}>
          {producto.nombre}
        </Text>

        {producto.variantes.map(variante => {
          const activa = variante.id === elegida.id;
          const disponible = true; // el menú público ya filtra las no disponibles (etapa 11)
          return (
            <Pressable
              key={variante.id}
              onPress={() => disponible && setVarianteElegida(variante)}
              style={{
                minHeight: 52,
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingHorizontal: spacing.md,
                borderRadius: radii.control,
                borderWidth: 1.5,
                borderColor: activa ? tokens['color.primary'] : tokens['color.border'],
                backgroundColor: activa ? tokens['color.surface'] : 'transparent',
                marginBottom: spacing.sm,
              }}>
              <Text style={{color: tokens['color.on-surface'], fontWeight: activa ? '700' : '400'}}>
                {variante.nombre}
              </Text>
              <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>
                {formatearMonto(variante.precio)}
              </Text>
            </Pressable>
          );
        })}
      </ScrollView>

      <Button
        titulo={`Agregar · ${formatearMonto(elegida.precio)}`}
        onPress={() => {
          agregar(producto, elegida);
          navigation.goBack();
        }}
      />
    </ScreenContainer>
  );
}
