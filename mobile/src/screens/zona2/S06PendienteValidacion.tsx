import React, {useEffect, useState} from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {BannerSinConexion} from '../../components/BannerSinConexion';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {misEstadiasVigentes} from '../../api/estadias';
import type {RootScreenProps} from '../../navigation/types';

const POLL_MS = 5000;
const MINUTOS_PARA_ESCALAR = 5;

/**
 * La pantalla más "abandono-prone" del flujo (etapa 07): 3 antídotos - ETA
 * concreta, barra de progreso visible, y una acción productiva (armar el
 * carrito mientras se espera; el pago solo se habilita con la estadía
 * ACTIVA, ver guard en S15). Polling cada 5s como contrato de primera clase
 * (ADR-003) - el SSE (`estadia.validada`) es una aceleración, no lo único.
 */
export function S06PendienteValidacion({route, navigation}: RootScreenProps<'S06PendienteValidacion'>) {
  const {estadiaPublicId} = route.params;
  const {tokens, spacing, typeScale, radii} = useTheme();
  const setEstadia = useStayStore(s => s.setEstadia);
  const cantidadEnCarrito = useCartStore(s => s.cantidadTotal());
  const [segundosEsperando, setSegundosEsperando] = useState(0);

  const vigentesQuery = useQuery({
    queryKey: ['estadias-vigentes'],
    queryFn: misEstadiasVigentes,
    refetchInterval: POLL_MS,
  });

  useEffect(() => {
    const intervalo = setInterval(() => setSegundosEsperando(s => s + 1), 1000);
    return () => clearInterval(intervalo);
  }, []);

  useEffect(() => {
    const estadia = (vigentesQuery.data ?? []).find(e => e.publicId === estadiaPublicId);
    if (!estadia) {
      return;
    }
    if (estadia.estado === 'ACTIVA') {
      setEstadia(estadia);
      navigation.replace('S07EstadiaActiva', {estadiaPublicId});
    } else if (estadia.estado === 'RECHAZADA') {
      navigation.replace('S08SolicitudRechazada', {estadiaPublicId, motivo: estadia.motivoRechazo});
    }
  }, [vigentesQuery.data, estadiaPublicId, navigation, setEstadia]);

  const escalado = segundosEsperando >= MINUTOS_PARA_ESCALAR * 60;

  return (
    <ScreenContainer>
      <BannerSinConexion />
      <View style={styles.centro}>
        <StateRow tipo="espera" texto="Esperando validación del carpero" tamano={28} />
        <Text style={{color: tokens['color.on-surface-muted'], marginTop: spacing.sm, marginBottom: spacing.lg}}>
          Normalmente tarda menos de 2 minutos
        </Text>

        <View
          style={[
            styles.barraFondo,
            {backgroundColor: tokens['color.border'], borderRadius: radii.pill, marginBottom: spacing.xl},
          ]}>
          <View
            style={[
              styles.barraProgreso,
              {backgroundColor: tokens['color.primary'], borderRadius: radii.pill, width: '60%'},
            ]}
          />
        </View>

        <Button
          titulo={cantidadEnCarrito > 0 ? `Seguir armando tu pedido (${cantidadEnCarrito})` : 'Armá tu pedido mientras esperás'}
          onPress={() => navigation.navigate('MainTabs')}
          variante="secundario"
        />

        {escalado ? (
          <View style={{marginTop: spacing.xl, gap: spacing.sm}}>
            <Text style={{color: tokens['color.on-surface-muted'], fontSize: typeScale.label}}>
              Está tardando más de lo esperado.
            </Text>
            <Button titulo="Avisar otra vez" onPress={() => vigentesQuery.refetch()} variante="contorno" />
          </View>
        ) : null}
      </View>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  centro: {flex: 1, alignItems: 'center', justifyContent: 'center'},
  barraFondo: {width: '100%', height: 8, overflow: 'hidden'},
  barraProgreso: {height: 8},
});
