import {apiRequest} from './client';
import type {SolicitudServicioResponse, TipoServicioResponse} from './types';

/** Resuelve el balneario desde la propia estadía del cliente (etapa 14), no por slug. */
export function tiposDeServicio(estadiaPublicId: string): Promise<TipoServicioResponse[]> {
  return apiRequest<TipoServicioResponse[]>(`/tipos-servicio?estadiaPublicId=${encodeURIComponent(estadiaPublicId)}`);
}

export function solicitarServicio(
  estadiaPublicId: string,
  tipoServicioId: number,
  nota: string | null,
): Promise<SolicitudServicioResponse> {
  return apiRequest<SolicitudServicioResponse>('/solicitudes-servicio', {
    method: 'POST',
    body: {estadiaPublicId, tipoServicioId, nota},
  });
}

export function solicitudesDeEstadia(estadiaPublicId: string): Promise<SolicitudServicioResponse[]> {
  return apiRequest<SolicitudServicioResponse[]>(
    `/solicitudes-servicio?estadiaPublicId=${encodeURIComponent(estadiaPublicId)}`,
  );
}

export function cancelarServicio(publicId: string): Promise<SolicitudServicioResponse> {
  return apiRequest<SolicitudServicioResponse>(`/solicitudes-servicio/${publicId}/cancelacion`, {method: 'POST'});
}
