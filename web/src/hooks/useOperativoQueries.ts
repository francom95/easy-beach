import { useQuery } from '@tanstack/react-query';
import { colaPedidos } from '../api/pedidos';
import { colaSolicitudesServicio } from '../api/servicios';
import { pendientesDeValidacion } from '../api/estadias';

/**
 * Polling como contrato de primera clase (ADR-003, mismo criterio que
 * mobile etapa 16): SSE (useOperativoRealtime) solo acelera invalidando
 * estas mismas queryKeys - si falla, esto solo sigue andando cada 8s.
 */
const INTERVALO_MS = 8000;

export function useColaPedidos() {
  return useQuery({ queryKey: ['operativo-pedidos'], queryFn: colaPedidos, refetchInterval: INTERVALO_MS });
}

export function useColaServicios() {
  return useQuery({ queryKey: ['operativo-servicios'], queryFn: colaSolicitudesServicio, refetchInterval: INTERVALO_MS });
}

export function useEstadiasPendientes() {
  return useQuery({ queryKey: ['operativo-estadias-pendientes'], queryFn: pendientesDeValidacion, refetchInterval: INTERVALO_MS });
}
