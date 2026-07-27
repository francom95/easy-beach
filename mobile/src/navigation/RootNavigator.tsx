import React, {useEffect, useRef} from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {useAuthStore} from '../store/authStore';
import {navigationRef} from './navigationRef';
import {EstadoCargando} from '../components/EstadoCarga';
import type {RootStackParamList} from './types';

import {S01Login} from '../screens/zona1/S01Login';
import {S02SelectorBalneario} from '../screens/zona1/S02SelectorBalneario';
import {S03Transicion} from '../screens/zona1/S03Transicion';
import {S04SplashBalneario} from '../screens/zona1/S04SplashBalneario';

import {S05ElegirUbicacion} from '../screens/zona2/S05ElegirUbicacion';
import {S06PendienteValidacion} from '../screens/zona2/S06PendienteValidacion';
import {S07EstadiaActiva} from '../screens/zona2/S07EstadiaActiva';
import {S08SolicitudRechazada} from '../screens/zona2/S08SolicitudRechazada';

import {MainTabs} from './MainTabs';
import {S12DetalleProductoSimple} from '../screens/zona3/S12DetalleProductoSimple';
import {S13DetalleConVariantes} from '../screens/zona3/S13DetalleConVariantes';
import {S14ProductoAgotado} from '../screens/zona3/S14ProductoAgotado';
import {S15Carrito} from '../screens/zona3/S15Carrito';
import {S16HojaMedioPago} from '../screens/zona3/S16HojaMedioPago';
import {S17ProcesandoPago} from '../screens/zona3/S17ProcesandoPago';
import {S18PagoAprobado} from '../screens/zona3/S18PagoAprobado';
import {S19PagoRechazado} from '../screens/zona3/S19PagoRechazado';
import {S20DetallePedido} from '../screens/zona3/S20DetallePedido';
import {S22PedidoCancelado} from '../screens/zona3/S22PedidoCancelado';
import {S23ServiciosHoja} from '../screens/zona3/S23ServiciosHoja';
import {S26SeccionPromociones} from '../screens/zona3/S26SeccionPromociones';
import {S27DetalleCombo} from '../screens/zona3/S27DetalleCombo';

import {S29ResumenConsumo} from '../screens/zona4/S29ResumenConsumo';
import {S30ConfirmarCierre} from '../screens/zona4/S30ConfirmarCierre';
import {S31EstadiaCerrada} from '../screens/zona4/S31EstadiaCerrada';
import {S32CierreBloqueado} from '../screens/zona4/S32CierreBloqueado';
import {S33SinConexion} from '../screens/zona4/S33SinConexion';
import {S34FueraDeTemporada} from '../screens/zona4/S34FueraDeTemporada';
import {S36CerradaPorBalneario} from '../screens/zona4/S36CerradaPorBalneario';

const Stack = createNativeStackNavigator<RootStackParamList>();

/**
 * Un único stack raíz (ver navigation/types.ts): más simple que anidar un
 * navigator por zona, y evita duplicar la lógica de "a qué pantalla entro
 * primero" en varios lugares. `initialRouteName` se fija UNA vez al montar
 * (cuando `estadoAuth` deja de ser 'cargando'); pérdidas de sesión
 * posteriores se manejan con un `reset()` explícito acá abajo, no
 * remontando el navigator.
 */
export function RootNavigator() {
  const estadoAuth = useAuthStore(s => s.estado);
  const restaurarSesion = useAuthStore(s => s.restaurarSesion);
  const estadoAuthAnterior = useRef(estadoAuth);

  useEffect(() => {
    restaurarSesion();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (
      estadoAuthAnterior.current === 'autenticado' &&
      estadoAuth === 'anonimo' &&
      navigationRef.isReady()
    ) {
      // La sesión se perdió en medio del uso (refresh token vencido/revocado,
      // ver authStore) - vuelve a S01, no queda la app en un estado raro.
      navigationRef.reset({index: 0, routes: [{name: 'S01Login'}]});
    }
    estadoAuthAnterior.current = estadoAuth;
  }, [estadoAuth]);

  if (estadoAuth === 'cargando') {
    return <EstadoCargando texto="Iniciando EasyBeach..." />;
  }

  return (
    <Stack.Navigator
      initialRouteName={estadoAuth === 'autenticado' ? 'S02SelectorBalneario' : 'S01Login'}
      screenOptions={{headerShown: false}}>
      <Stack.Screen name="S01Login" component={S01Login} />
      <Stack.Screen name="S02SelectorBalneario" component={S02SelectorBalneario} />
      <Stack.Screen name="S03Transicion" component={S03Transicion} options={{animation: 'fade'}} />
      <Stack.Screen name="S04SplashBalneario" component={S04SplashBalneario} />

      <Stack.Screen name="S05ElegirUbicacion" component={S05ElegirUbicacion} />
      <Stack.Screen name="S06PendienteValidacion" component={S06PendienteValidacion} />
      <Stack.Screen name="S07EstadiaActiva" component={S07EstadiaActiva} />
      <Stack.Screen name="S08SolicitudRechazada" component={S08SolicitudRechazada} />

      <Stack.Screen name="MainTabs" component={MainTabs} />
      <Stack.Screen name="S12DetalleProductoSimple" component={S12DetalleProductoSimple} />
      <Stack.Screen name="S13DetalleConVariantes" component={S13DetalleConVariantes} />
      <Stack.Screen name="S14ProductoAgotado" component={S14ProductoAgotado} />
      <Stack.Screen name="S15Carrito" component={S15Carrito} />
      <Stack.Screen name="S16HojaMedioPago" component={S16HojaMedioPago} options={{presentation: 'modal'}} />
      <Stack.Screen
        name="S17ProcesandoPago"
        component={S17ProcesandoPago}
        options={{gestureEnabled: false}}
      />
      <Stack.Screen name="S18PagoAprobado" component={S18PagoAprobado} options={{gestureEnabled: false}} />
      <Stack.Screen name="S19PagoRechazado" component={S19PagoRechazado} />
      <Stack.Screen name="S20DetallePedido" component={S20DetallePedido} />
      <Stack.Screen name="S22PedidoCancelado" component={S22PedidoCancelado} />
      <Stack.Screen name="S23ServiciosHoja" component={S23ServiciosHoja} options={{presentation: 'modal'}} />
      <Stack.Screen name="S26SeccionPromociones" component={S26SeccionPromociones} />
      <Stack.Screen name="S27DetalleCombo" component={S27DetalleCombo} />

      <Stack.Screen name="S29ResumenConsumo" component={S29ResumenConsumo} />
      <Stack.Screen name="S30ConfirmarCierre" component={S30ConfirmarCierre} options={{presentation: 'modal'}} />
      <Stack.Screen name="S31EstadiaCerrada" component={S31EstadiaCerrada} options={{gestureEnabled: false}} />
      <Stack.Screen name="S32CierreBloqueado" component={S32CierreBloqueado} options={{presentation: 'modal'}} />
      <Stack.Screen name="S33SinConexion" component={S33SinConexion} />
      <Stack.Screen name="S34FueraDeTemporada" component={S34FueraDeTemporada} />
      <Stack.Screen name="S36CerradaPorBalneario" component={S36CerradaPorBalneario} options={{gestureEnabled: false}} />
    </Stack.Navigator>
  );
}
