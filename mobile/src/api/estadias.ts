import {apiRequest} from './client';
import type {EstadiaResponse, ResumenCierreResponse} from './types';

export function solicitarEstadia(balnearioSlug: string, ubicacionId: number): Promise<EstadiaResponse> {
  return apiRequest<EstadiaResponse>('/estadias', {
    method: 'POST',
    body: {balnearioSlug, ubicacionId},
  });
}

/** Las que ocupan cupo (PENDIENTE_VALIDACION o ACTIVA), en todos los balnearios donde el cliente tenga una. */
export function misEstadiasVigentes(): Promise<EstadiaResponse[]> {
  return apiRequest<EstadiaResponse[]>('/estadias/vigentes');
}

export function miHistorialDeEstadias(): Promise<EstadiaResponse[]> {
  return apiRequest<EstadiaResponse[]>('/estadias/historial');
}

export function cambiarUbicacion(estadiaPublicId: string, ubicacionId: number): Promise<EstadiaResponse> {
  return apiRequest<EstadiaResponse>(`/estadias/${estadiaPublicId}/ubicacion`, {
    method: 'PUT',
    body: {ubicacionId},
  });
}

export function cerrarEstadia(estadiaPublicId: string): Promise<ResumenCierreResponse> {
  return apiRequest<ResumenCierreResponse>(`/estadias/${estadiaPublicId}/cierre`, {method: 'POST'});
}
