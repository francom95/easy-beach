import React from 'react';
import {Linking, Text, View} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {obtenerBalneario} from '../../api/balnearios';
import {solicitarEstadia} from '../../api/estadias';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * "Nunca es culpa del cliente, nunca un callejón sin salida" (etapa 07): se
 * explica la causa probable, se puede reintentar en el lugar, y hay una
 * salida humana. El carrito armado antes del rechazo sigue intacto (vive en
 * cartStore, ajeno a esta pantalla).
 */
export function S08SolicitudRechazada({route, navigation}: RootScreenProps<'S08SolicitudRechazada'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const {estadia, balnearioSlug} = useStayStore(s => ({estadia: s.estadia, balnearioSlug: s.balnearioSlug}));
  const setEstadia = useStayStore(s => s.setEstadia);

  const balnearioQuery = useQuery({
    queryKey: ['balneario', balnearioSlug],
    queryFn: () => obtenerBalneario(balnearioSlug!),
    enabled: Boolean(balnearioSlug),
  });

  const reintentarMutation = useMutation({
    mutationFn: () => solicitarEstadia(balnearioSlug!, estadia!.ubicacionId),
    onSuccess: nuevaEstadia => {
      setEstadia(nuevaEstadia);
      navigation.replace('S06PendienteValidacion', {estadiaPublicId: nuevaEstadia.publicId});
    },
  });

  function llamarACarpero() {
    const telefono = balnearioQuery.data?.telefono;
    if (telefono) {
      Linking.openURL(`tel:${telefono}`);
    }
  }

  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="advertencia" texto="No pudimos confirmar tu ubicación" tamano={32} />
        <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.body}}>
          {route.params.motivo ?? 'Puede que la ubicación ya esté ocupada o no esté disponible.'}
        </Text>

        <Button
          titulo="Elegir otra ubicación"
          onPress={() => navigation.replace('S05ElegirUbicacion')}
          cargando={false}
        />
        <Button
          titulo="Reintentar la misma ubicación"
          onPress={() => reintentarMutation.mutate()}
          cargando={reintentarMutation.isPending}
          variante="secundario"
        />
        {balnearioQuery.data?.telefono ? (
          <Button titulo="Llamar a un carpero" onPress={llamarACarpero} variante="contorno" />
        ) : null}

        {reintentarMutation.isError ? <EstadoError mensaje={(reintentarMutation.error as ApiError).detail} /> : null}
      </View>
    </ScreenContainer>
  );
}
