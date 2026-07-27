import React, {useMemo, useState} from 'react';
import {FlatList, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {listarBalneariosOperativos} from '../../api/balnearios';
import {misEstadiasVigentes} from '../../api/estadias';
import type {BalnearioPublicoResponse, EstadiaResponse} from '../../api/types';
import type {RootScreenProps} from '../../navigation/types';

type BalnearioConEstadia = BalnearioPublicoResponse & {estadiaVigente?: EstadiaResponse};

/**
 * Geolocalización solo para ordenar por distancia, NUNCA un requisito
 * (etapa 07) - no se pide permiso de ubicación en esta etapa; se deja
 * como mejora futura documentada en el entregable, orden alfabético por
 * ahora. Las estadías abiertas van pinneadas arriba (un cliente puede
 * tener varias, en balnearios distintos - etapa 12).
 */
export function S02SelectorBalneario({navigation}: RootScreenProps<'S02SelectorBalneario'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const [busqueda, setBusqueda] = useState('');

  const balneariosQuery = useQuery({queryKey: ['balnearios'], queryFn: listarBalneariosOperativos});
  const vigentesQuery = useQuery({queryKey: ['estadias-vigentes'], queryFn: misEstadiasVigentes});

  const items = useMemo<BalnearioConEstadia[]>(() => {
    const balnearios = balneariosQuery.data ?? [];
    const vigentes = vigentesQuery.data ?? [];
    const conEstadia = balnearios.map(b => ({
      ...b,
      estadiaVigente: vigentes.find(e => e.balnearioId === b.id),
    }));
    const filtrados = busqueda.trim()
      ? conEstadia.filter(b => b.nombre.toLowerCase().includes(busqueda.trim().toLowerCase()))
      : conEstadia;
    return [...filtrados].sort((a, b) => {
      if (Boolean(a.estadiaVigente) !== Boolean(b.estadiaVigente)) {
        return a.estadiaVigente ? -1 : 1;
      }
      return a.nombre.localeCompare(b.nombre);
    });
  }, [balneariosQuery.data, vigentesQuery.data, busqueda]);

  function elegir(balneario: BalnearioConEstadia) {
    navigation.navigate('S03Transicion', {
      slug: balneario.slug,
      nombre: balneario.nombre,
      colorOrigen: tokens['color.primary']!,
    });
  }

  if (balneariosQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando texto="Buscando balnearios..." />
      </ScreenContainer>
    );
  }
  if (balneariosQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar los balnearios." onReintentar={() => balneariosQuery.refetch()} />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.md}}>
        Elegí tu balneario
      </Text>
      <TextInput
        placeholder="Buscar..."
        value={busqueda}
        onChangeText={setBusqueda}
        placeholderTextColor={tokens['color.on-surface-muted']}
        style={{
          borderWidth: 1.5,
          borderColor: tokens['color.border'],
          borderRadius: radii.control,
          padding: spacing.md,
          marginBottom: spacing.md,
          color: tokens['color.on-surface'],
        }}
      />
      {items.length === 0 ? (
        <EstadoVacio titulo="No encontramos balnearios" descripcion="Probá con otra búsqueda." />
      ) : (
        <FlatList
          data={items}
          keyExtractor={item => item.slug}
          contentContainerStyle={{gap: spacing.md}}
          renderItem={({item}) => (
            <Pressable
              onPress={() => elegir(item)}
              style={[
                styles.card,
                {
                  backgroundColor: tokens['color.surface'],
                  borderColor: tokens['color.border'],
                  borderRadius: radii.card,
                  padding: spacing.lg,
                },
              ]}>
              <Text style={{fontSize: typeScale.body, fontWeight: '700', color: tokens['color.on-surface']}}>
                {item.nombre}
              </Text>
              {item.estadiaVigente ? (
                <View style={{marginTop: spacing.xs}}>
                  <StateRow
                    tipo={item.estadiaVigente.estado === 'ACTIVA' ? 'exito' : 'espera'}
                    texto={item.estadiaVigente.estado === 'ACTIVA' ? 'Estadía abierta' : 'Validación pendiente'}
                  />
                </View>
              ) : null}
            </Pressable>
          )}
        />
      )}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  card: {borderWidth: StyleSheet.hairlineWidth},
});
