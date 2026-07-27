import { apiRequest } from './client';
import type { EstadoUbicacion, TipoUbicacion, UbicacionResponse } from './types';

export function listarUbicaciones(): Promise<UbicacionResponse[]> {
  return apiRequest<UbicacionResponse[]>('/admin/ubicaciones');
}

export function crearUbicacion(tipo: TipoUbicacion, identificador: string): Promise<UbicacionResponse> {
  return apiRequest<UbicacionResponse>('/admin/ubicaciones', { method: 'POST', body: { tipo, identificador } });
}

export function actualizarUbicacion(id: number, tipo: TipoUbicacion, identificador: string): Promise<UbicacionResponse> {
  return apiRequest<UbicacionResponse>(`/admin/ubicaciones/${id}`, { method: 'PUT', body: { tipo, identificador } });
}

export function cambiarEstadoUbicacion(id: number, estado: EstadoUbicacion): Promise<UbicacionResponse> {
  return apiRequest<UbicacionResponse>(`/admin/ubicaciones/${id}/estado`, { method: 'PUT', body: { estado } });
}

export function eliminarUbicacion(id: number): Promise<void> {
  return apiRequest<void>(`/admin/ubicaciones/${id}`, { method: 'DELETE' });
}

/** "Alta masiva 1→N" (mockup etapa 08): sin endpoint bulk en el backend - se resuelve en cliente, una request por ubicación. */
export async function crearUbicacionesMasivo(tipo: TipoUbicacion, prefijo: string, desde: number, hasta: number): Promise<UbicacionResponse[]> {
  const resultados: UbicacionResponse[] = [];
  for (let n = desde; n <= hasta; n++) {
    resultados.push(await crearUbicacion(tipo, `${prefijo}${n}`));
  }
  return resultados;
}
