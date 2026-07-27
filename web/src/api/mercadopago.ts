import { apiRequest } from './client';
import type { EstadoVinculacionResponse, IniciarVinculacionResponse } from './types';

export function estadoVinculacionMp(): Promise<EstadoVinculacionResponse> {
  return apiRequest<EstadoVinculacionResponse>('/admin/mercadopago/estado');
}

export function iniciarVinculacionMp(): Promise<IniciarVinculacionResponse> {
  return apiRequest<IniciarVinculacionResponse>('/admin/mercadopago/oauth/iniciar');
}

export function desvincularMp(): Promise<void> {
  return apiRequest<void>('/admin/mercadopago/desvincular', { method: 'POST' });
}
