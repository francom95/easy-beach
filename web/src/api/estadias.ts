import { apiRequest } from './client';
import type { EstadiaPendienteResponse, EstadiaResponse } from './types';

export function pendientesDeValidacion(): Promise<EstadiaPendienteResponse[]> {
  return apiRequest<EstadiaPendienteResponse[]>('/operativo/estadias/pendientes');
}

export function validarEstadia(publicId: string): Promise<EstadiaResponse> {
  return apiRequest<EstadiaResponse>(`/operativo/estadias/${publicId}/validacion`, { method: 'POST' });
}

export function rechazarEstadia(publicId: string, motivo: string): Promise<EstadiaResponse> {
  return apiRequest<EstadiaResponse>(`/operativo/estadias/${publicId}/rechazo`, { method: 'POST', body: { motivo } });
}
