import React, {useState} from 'react';
import {Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoCargando, EstadoError, EstadoVacio} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {tiposDeServicio, solicitarServicio, solicitudesDeEstadia} from '../../api/servicios';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * S23 "hoja de servicios" y S24 "en curso" son la MISMA pantalla (etapa 07:
 * "no hay navegación nueva, se reemplaza el contenido"). Ubicación heredada
 * de la estadía - no se pregunta nada más. "Otra cosa" (nota libre) existe
 * pero al final, sin competir con los 4 tipos predefinidos.
 */
export function S23ServiciosHoja({navigation}: RootScreenProps<'S23ServiciosHoja'>) {
  const {tokens, spacing, typeScale, radii} = useTheme();
  const estadia = useStayStore(s => s.estadia)!;
  const [nota, setNota] = useState('');

  const tiposQuery = useQuery({
    queryKey: ['tipos-servicio', estadia.publicId],
    queryFn: () => tiposDeServicio(estadia.publicId),
  });
  const solicitudesQuery = useQuery({
    queryKey: ['solicitudes-servicio', estadia.publicId],
    queryFn: () => solicitudesDeEstadia(estadia.publicId),
    refetchInterval: 5000,
  });

  const solicitarMutation = useMutation({
    mutationFn: (tipoServicioId: number) => solicitarServicio(estadia.publicId, tipoServicioId, nota || null),
    onSuccess: () => solicitudesQuery.refetch(),
  });

  const solicitudEnCurso = (solicitudesQuery.data ?? []).find(s => s.estado === 'PENDIENTE' || s.estado === 'EN_CURSO');

  if (solicitudEnCurso) {
    return (
      <ScreenContainer>
        <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md}}>
          <StateRow
            tipo="espera"
            texto={solicitudEnCurso.estado === 'EN_CURSO' ? 'Un carpero está en camino' : 'Solicitud enviada'}
            tamano={32}
          />
          <Text style={{color: tokens['color.on-surface-muted']}}>{solicitudEnCurso.tipoServicioNombre}</Text>
          <Button titulo="Cerrar" onPress={() => navigation.goBack()} variante="contorno" fullWidth={false} />
        </View>
      </ScreenContainer>
    );
  }

  if (tiposQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }
  if (tiposQuery.isError) {
    return (
      <ScreenContainer>
        <EstadoError mensaje="No pudimos cargar los servicios." onReintentar={() => tiposQuery.refetch()} />
      </ScreenContainer>
    );
  }
  if ((tiposQuery.data ?? []).length === 0) {
    return (
      <ScreenContainer>
        <EstadoVacio titulo="Este balneario no tiene servicios configurados" />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.lg}}>
        ¿Qué necesitás?
      </Text>
      <View style={[styles.grilla, {gap: spacing.md}]}>
        {(tiposQuery.data ?? []).map(tipo => (
          <Pressable
            key={tipo.id}
            onPress={() => solicitarMutation.mutate(tipo.id)}
            disabled={solicitarMutation.isPending}
            style={[
              styles.tile,
              {backgroundColor: tokens['color.surface'], borderColor: tokens['color.border'], borderRadius: radii.card},
            ]}>
            <Text style={{color: tokens['color.on-surface'], fontWeight: '700', textAlign: 'center'}}>{tipo.nombre}</Text>
          </Pressable>
        ))}
      </View>

      <TextInput
        placeholder="Otra cosa (opcional)"
        value={nota}
        onChangeText={setNota}
        placeholderTextColor={tokens['color.on-surface-muted']}
        style={{
          marginTop: spacing.xl,
          borderWidth: 1.5,
          borderColor: tokens['color.border'],
          borderRadius: radii.control,
          padding: spacing.md,
          color: tokens['color.on-surface'],
        }}
      />
      {solicitarMutation.isError ? (
        <View style={{marginTop: spacing.md}}>
          <EstadoError mensaje={(solicitarMutation.error as ApiError).detail} />
        </View>
      ) : null}
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  grilla: {flexDirection: 'row', flexWrap: 'wrap'},
  tile: {width: 116, height: 116, alignItems: 'center', justifyContent: 'center', padding: 8, borderWidth: StyleSheet.hairlineWidth},
});
