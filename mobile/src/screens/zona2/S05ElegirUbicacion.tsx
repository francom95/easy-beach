import React, {useMemo, useState} from 'react';
import {FlatList, Pressable, StyleSheet, Text, View} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {listarUbicaciones} from '../../api/balnearios';
import {solicitarEstadia} from '../../api/estadias';
import {ApiError} from '../../api/ApiError';
import {TipoUbicacion, type UbicacionResponse} from '../../api/types';
import type {RootScreenProps} from '../../navigation/types';

const ETIQUETA_TIPO: Record<TipoUbicacion, string> = {
  [TipoUbicacion.CARPA]: 'Carpa',
  [TipoUbicacion.SOMBRILLA]: 'Sombrilla',
  [TipoUbicacion.MESA]: 'Mesa',
  [TipoUbicacion.SECTOR]: 'Sector',
};

/**
 * El mockup de la etapa 07 propone 3 targets grandes (Carpa/Sombrilla/Mesa)
 * + teclado numérico o chips según el tipo. El modelo real del backend
 * (etapa 11) es más simple: cada ubicación es un par (tipo, identificador de
 * texto libre) sin distinguir "número" de "chip de sector" - se adapta acá
 * agrupando por tipo y listando los identificadores reales como filas
 * tocables, en vez de reconstruir una entrada numérica que no aplicaría a
 * identificadores no numéricos (ej. "Sector B").
 */
export function S05ElegirUbicacion({navigation}: RootScreenProps<'S05ElegirUbicacion'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const setEstadia = useStayStore(s => s.setEstadia);

  const [tipoElegido, setTipoElegido] = useState<TipoUbicacion | null>(null);

  const ubicacionesQuery = useQuery({
    queryKey: ['ubicaciones', balnearioSlug],
    queryFn: () => listarUbicaciones(balnearioSlug),
  });

  const solicitarMutation = useMutation({
    mutationFn: (ubicacionId: number) => solicitarEstadia(balnearioSlug, ubicacionId),
    onSuccess: estadia => {
      setEstadia(estadia);
      navigation.replace('S06PendienteValidacion', {estadiaPublicId: estadia.publicId});
    },
  });

  const porTipo = useMemo(() => {
    const grupos = new Map<TipoUbicacion, UbicacionResponse[]>();
    for (const ubicacion of ubicacionesQuery.data ?? []) {
      const lista = grupos.get(ubicacion.tipo) ?? [];
      lista.push(ubicacion);
      grupos.set(ubicacion.tipo, lista);
    }
    return grupos;
  }, [ubicacionesQuery.data]);

  const tiposDisponibles = Array.from(porTipo.keys());

  if (ubicacionesQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando texto="Buscando ubicaciones..." />
      </ScreenContainer>
    );
  }
  if (ubicacionesQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar las ubicaciones." onReintentar={() => ubicacionesQuery.refetch()} />
      </ScreenContainer>
    );
  }
  if (tiposDisponibles.length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio
          titulo="No hay ubicaciones configuradas"
          descripcion="Pedile a un carpero que te ayude a abrir tu estadía."
        />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer scroll>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.lg}}>
        ¿Dónde estás?
      </Text>

      <View style={[styles.filaTipos, {gap: spacing.md, marginBottom: spacing.lg}]}>
        {tiposDisponibles.map(tipo => {
          const activo = tipoElegido === tipo;
          return (
            <Pressable
              key={tipo}
              onPress={() => setTipoElegido(tipo)}
              style={[
                styles.tarjetaTipo,
                {
                  backgroundColor: activo ? tokens['color.primary'] : tokens['color.surface'],
                  borderColor: tokens['color.border'],
                  borderRadius: radii.card,
                },
              ]}>
              <Text
                style={{
                  fontWeight: '700',
                  color: activo ? tokens['color.on-primary'] : tokens['color.on-surface'],
                }}>
                {ETIQUETA_TIPO[tipo]}
              </Text>
            </Pressable>
          );
        })}
      </View>

      {tipoElegido ? (
        <FlatList
          data={porTipo.get(tipoElegido) ?? []}
          keyExtractor={item => String(item.id)}
          scrollEnabled={false}
          contentContainerStyle={{gap: spacing.sm}}
          renderItem={({item}) => (
            <Pressable
              onPress={() => solicitarMutation.mutate(item.id)}
              disabled={solicitarMutation.isPending}
              style={[
                styles.filaUbicacion,
                {
                  backgroundColor: tokens['color.surface'],
                  borderColor: tokens['color.border'],
                  borderRadius: radii.control,
                  padding: spacing.md,
                },
              ]}>
              <Text style={{color: tokens['color.on-surface'], fontSize: typeScale.body}}>{item.identificador}</Text>
            </Pressable>
          )}
        />
      ) : null}

      {solicitarMutation.isError ? (
        <View style={{marginTop: spacing.md}}>
          <EstadoError mensaje={(solicitarMutation.error as ApiError).detail} />
        </View>
      ) : null}
      {solicitarMutation.isPending ? (
        <View style={{marginTop: spacing.md}}>
          <Button titulo="Abriendo tu estadía..." onPress={() => {}} cargando deshabilitado />
        </View>
      ) : null}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  filaTipos: {flexDirection: 'row', flexWrap: 'wrap'},
  tarjetaTipo: {minWidth: 92, minHeight: 92, alignItems: 'center', justifyContent: 'center', borderWidth: StyleSheet.hairlineWidth, paddingHorizontal: 12},
  filaUbicacion: {borderWidth: StyleSheet.hairlineWidth},
});
