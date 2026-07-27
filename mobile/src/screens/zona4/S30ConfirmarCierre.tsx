import React from 'react';
import {Text, View} from 'react-native';
import {useMutation} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoError} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {cerrarEstadia} from '../../api/estadias';
import {ApiError} from '../../api/ApiError';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Explica la consecuencia real (hay que revalidar para reabrir), no un
 * "¿seguro?" genérico. El botón destructivo va relleno (etapa 07: lee
 * distinto a propósito, es el único caso). Si el servidor bloquea el
 * cierre (409 - apareció un pedido en curso entre S29 y acá), se manda a
 * S32 en vez de mostrar un error crudo.
 */
export function S30ConfirmarCierre({navigation}: RootScreenProps<'S30ConfirmarCierre'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const estadia = useStayStore(s => s.estadia)!;

  const cerrarMutation = useMutation({
    mutationFn: () => cerrarEstadia(estadia.publicId),
    onSuccess: resumen => {
      navigation.replace('S31EstadiaCerrada', {
        resumen: {
          diasDeEstadia: resumen.diasDeEstadia,
          cantidadPedidos: resumen.cantidadPedidos,
          montoTotal: resumen.montoTotal,
        },
      });
    },
    onError: error => {
      if ((error as ApiError).esConflicto) {
        navigation.replace('S32CierreBloqueado');
      }
    },
  });

  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.lg}}>
        <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background']}}>
          ¿Cerrar tu estadía?
        </Text>
        <Text style={{color: tokens['color.on-surface-muted']}}>
          Si volvés más tarde, un carpero va a tener que confirmar tu ubicación de nuevo - no es automático.
        </Text>

        <Button
          titulo="Sí, cerrar estadía"
          variante="destructivo"
          onPress={() => cerrarMutation.mutate()}
          cargando={cerrarMutation.isPending}
        />
        <Button titulo="No, seguir en la playa" onPress={() => navigation.goBack()} variante="contorno" />

        {cerrarMutation.isError && !(cerrarMutation.error as ApiError).esConflicto ? (
          <EstadoError mensaje={(cerrarMutation.error as ApiError).detail} />
        ) : null}
      </View>
    </ScreenContainer>
  );
}
