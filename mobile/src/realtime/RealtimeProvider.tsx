import React, {useEffect} from 'react';
import EventSource from 'react-native-sse';
import {useQueryClient} from '@tanstack/react-query';
import {API_BASE_URL} from '../api/config';
import {getAccessToken} from '../auth/tokenHolder';
import {useAuthStore} from '../store/authStore';

type EventoNombre = 'estadia.validada' | 'pago.resultado' | 'pedido.estado' | 'servicio.estado';

/**
 * Cliente SSE de `/stream/cliente` (ADR-003). Reconexión automática nativa
 * de `react-native-sse` (polyfill de EventSource sobre XHR, ya que RN no
 * implementa `EventSource` nativo - por eso ADR-003 marcaba "validar en la
 * etapa 16" esta librería puntual).
 *
 * <p><b>No reemplaza el polling, lo acelera.</b> Cada pantalla (S06, S09,
 * S17, S20, S21, S23) ya hace polling por su cuenta con `refetchInterval`
 * de React Query - eso es lo que garantiza que TODO sea reconstruible por
 * GET aunque el SSE falle en la playa (contrato de primera clase de
 * ADR-003, no un plan B). Este provider solo invalida las queries
 * relevantes cuando llega un evento, para que el próximo render sea
 * instantáneo en vez de esperar el próximo intervalo de polling.
 *
 * <p><b>No verificado en un dispositivo/emulador real dentro de este
 * entorno</b> (ver el entregable de la etapa): la conexión SSE necesita un
 * backend corriendo y accesible, y no se pudo mantener una sesión larga
 * abierta durante la verificación. El polling SÍ se verificó real.
 */
export function RealtimeProvider({children}: {children: React.ReactNode}) {
  const queryClient = useQueryClient();
  const estadoAuth = useAuthStore(s => s.estado);

  useEffect(() => {
    if (estadoAuth !== 'autenticado') {
      return;
    }
    const token = getAccessToken();
    if (!token) {
      return;
    }

    const es = new EventSource(`${API_BASE_URL}/stream/cliente`, {
      headers: {Authorization: `Bearer ${token}`},
      pollingInterval: 0, // sin auto-poll interno de la librería - el reintento de conexión ya alcanza
    });

    const invalidar = (claves: string[]) => claves.forEach(clave => queryClient.invalidateQueries({queryKey: [clave]}));

    const manejador = (nombre: EventoNombre, claves: string[]) => () => invalidar(claves);

    es.addEventListener('estadia.validada' as never, manejador('estadia.validada', ['estadias-vigentes']));
    es.addEventListener('pago.resultado' as never, manejador('pago.resultado', ['pedido', 'pedidos']));
    es.addEventListener('pedido.estado' as never, manejador('pedido.estado', ['pedido', 'pedidos', 'pedido-historial']));
    es.addEventListener('servicio.estado' as never, manejador('servicio.estado', ['solicitudes-servicio']));

    return () => es.close();
  }, [estadoAuth, queryClient]);

  return <>{children}</>;
}
