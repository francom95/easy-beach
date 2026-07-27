import { apiRequest } from './client';
import type { EstadoPromocion, PromocionInput, PromocionResponse } from './types';

export function listarPromociones(): Promise<PromocionResponse[]> {
  return apiRequest<PromocionResponse[]>('/admin/promociones');
}

export function crearPromocion(input: PromocionInput): Promise<PromocionResponse> {
  return apiRequest<PromocionResponse>('/admin/promociones', { method: 'POST', body: input });
}

export function actualizarPromocion(id: number, input: PromocionInput): Promise<PromocionResponse> {
  return apiRequest<PromocionResponse>(`/admin/promociones/${id}`, { method: 'PUT', body: input });
}

export function cambiarEstadoPromocion(id: number, estado: EstadoPromocion): Promise<PromocionResponse> {
  return apiRequest<PromocionResponse>(`/admin/promociones/${id}/estado`, { method: 'PUT', body: { estado } });
}

export function eliminarPromocion(id: number): Promise<void> {
  return apiRequest<void>(`/admin/promociones/${id}`, { method: 'DELETE' });
}
