import { apiRequest } from './client';
import type { EstadoSolicitudServicio, SolicitudServicioResponse, TipoServicioResponse } from './types';

export function listarTiposServicio(): Promise<TipoServicioResponse[]> {
  return apiRequest<TipoServicioResponse[]>('/admin/tipos-servicio');
}

export function crearTipoServicio(nombre: string, activo: boolean, orden: number): Promise<TipoServicioResponse> {
  return apiRequest<TipoServicioResponse>('/admin/tipos-servicio', { method: 'POST', body: { nombre, activo, orden } });
}

export function actualizarTipoServicio(id: number, nombre: string, activo: boolean, orden: number): Promise<TipoServicioResponse> {
  return apiRequest<TipoServicioResponse>(`/admin/tipos-servicio/${id}`, { method: 'PUT', body: { nombre, activo, orden } });
}

export function eliminarTipoServicio(id: number): Promise<void> {
  return apiRequest<void>(`/admin/tipos-servicio/${id}`, { method: 'DELETE' });
}

export function colaSolicitudesServicio(): Promise<SolicitudServicioResponse[]> {
  return apiRequest<SolicitudServicioResponse[]>('/operativo/solicitudes-servicio/cola');
}

export function transicionarSolicitudServicio(publicId: string, estado: EstadoSolicitudServicio): Promise<SolicitudServicioResponse> {
  return apiRequest<SolicitudServicioResponse>(`/operativo/solicitudes-servicio/${publicId}/estado`, {
    method: 'PUT',
    body: { estado },
  });
}
