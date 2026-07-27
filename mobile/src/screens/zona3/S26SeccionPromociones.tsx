import React from 'react';
import {FlatList, Pressable, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Card} from '../../components/Card';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {listarPromociones} from '../../api/catalogo';
import type {RootScreenProps} from '../../navigation/types';

const ETIQUETA_MECANICA: Record<string, string> = {
  DESCUENTO_PORCENTUAL: 'Descuento',
  COMBO: 'Combo',
  HAPPY_HOUR: 'Happy hour',
};

/**
 * Las 3 mecánicas del MVP (etapa 14) se distinguen por ETIQUETA, no color
 * (regla de accesibilidad de la etapa 06). Sin countdown real: la API
 * pública no expone `vigenciaHasta` en `PromocionResumen` - se muestra el
 * nombre/tipo/valor tal cual vienen, sin inventar una cuenta regresiva que
 * el backend no sostiene.
 */
export function S26SeccionPromociones({navigation}: RootScreenProps<'S26SeccionPromociones'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const promosQuery = useQuery({queryKey: ['promociones', balnearioSlug], queryFn: () => listarPromociones(balnearioSlug)});

  if (promosQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }
  if (promosQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar las promociones." onReintentar={() => promosQuery.refetch()} />
      </ScreenContainer>
    );
  }
  if ((promosQuery.data ?? []).length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio titulo="No hay promociones activas ahora" descripcion="Volvé a mirar más tarde." />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.md}}>
        Promociones
      </Text>
      <FlatList
        data={promosQuery.data}
        keyExtractor={p => String(p.id)}
        contentContainerStyle={{gap: spacing.sm}}
        renderItem={({item}) => (
          <Pressable
            onPress={() => item.tipo === 'COMBO' && navigation.navigate('S27DetalleCombo', {promocionId: item.id})}>
            <Card>
              <View style={{flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'}}>
                <View>
                  <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>{item.nombre}</Text>
                  <View
                    style={{
                      marginTop: 4,
                      alignSelf: 'flex-start',
                      backgroundColor: tokens['color.background'],
                      borderRadius: radii.pill,
                      paddingHorizontal: 8,
                      paddingVertical: 2,
                    }}>
                    <Text style={{fontSize: 11, fontWeight: '700', color: tokens['color.on-surface-muted']}}>
                      {ETIQUETA_MECANICA[item.tipo] ?? item.tipo}
                    </Text>
                  </View>
                </View>
                <Text style={{fontWeight: '800', color: tokens['color.primary']}}>
                  {item.tipo === 'DESCUENTO_PORCENTUAL' || item.tipo === 'HAPPY_HOUR' ? `${item.valor}%` : `$${item.valor}`}
                </Text>
              </View>
            </Card>
          </Pressable>
        )}
      />
    </ScreenContainer>
  );
}
