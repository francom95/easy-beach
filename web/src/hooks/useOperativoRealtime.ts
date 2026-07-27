'use client';

import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { API_BASE_URL } from '../api/config';
import { getAccessToken } from '../auth/tokenHolder';
import { useAuthStore } from '../store/authStore';

type EventoNombre = 'pedido.nuevo' | 'pedido.estado' | 'servicio.nuevo' | 'servicio.estado';

const CLAVES_POR_EVENTO: Record<EventoNombre, string[]> = {
  'pedido.nuevo': ['operativo-pedidos'],
  'pedido.estado': ['operativo-pedidos'],
  'servicio.nuevo': ['operativo-servicios'],
  'servicio.estado': ['operativo-servicios'],
};

/**
 * Acelerador sobre el polling de useOperativoQueries (ADR-003) - nunca un
 * reemplazo. `EventSource` nativo no puede mandar el header `Authorization`
 * (limitación conocida del API del navegador), por eso `fetch-event-source`
 * en vez del `EventSource` del DOM - mismo problema que resolvió
 * `react-native-sse` del lado mobile (etapa 16).
 *
 * <p>No cubre la bandeja de validación de estadías: el canal `/stream/operativo`
 * no emite ningún evento de estadía (gap ya documentado en etapa 16/17) - esa
 * cola queda solo a polling.
 */
export function useOperativoRealtime() {
  const queryClient = useQueryClient();
  const estadoAuth = useAuthStore(s => s.estado);

  useEffect(() => {
    if (estadoAuth !== 'autenticado') return;
    const token = getAccessToken();
    if (!token) return;

    const controller = new AbortController();

    fetchEventSource(`${API_BASE_URL}/stream/operativo`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: controller.signal,
      async onopen() {
        // conexión abierta - nada que hacer, el polling ya cubre el estado inicial
      },
      onmessage(ev) {
        const nombre = ev.event as EventoNombre;
        const claves = CLAVES_POR_EVENTO[nombre];
        claves?.forEach(clave => queryClient.invalidateQueries({ queryKey: [clave] }));
      },
      onerror() {
        // fetch-event-source reintenta solo; no relanzar para no cortar el reintento automático
      },
    }).catch(() => {
      // conexión cerrada por el AbortController al desmontar - esperado
    });

    return () => controller.abort();
  }, [estadoAuth, queryClient]);
}
