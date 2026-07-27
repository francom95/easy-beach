import { apiRequest } from './client';
import type { BalnearioResponse } from './types';

export function obtenerMiBalneario(): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>('/admin/balneario');
}

/** Para CARPERO/OPERADOR (no solo ADMIN_BALNEARIO): header del panel operativo. */
export function obtenerMiBalnearioStaff(): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>('/staff/balneario');
}
