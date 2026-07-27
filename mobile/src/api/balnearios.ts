import {apiRequest} from './client';
import type {BalnearioPublicoResponse, BrandingTokens, UbicacionResponse} from './types';

export function listarBalneariosOperativos(): Promise<BalnearioPublicoResponse[]> {
  return apiRequest<BalnearioPublicoResponse[]>('/balnearios');
}

/** Lanza ApiError(404) si el slug no existe, o 409 BALNEARIO_NO_OPERATIVO si dejó de estar operativo (S34). */
export function obtenerBalneario(slug: string): Promise<BalnearioPublicoResponse> {
  return apiRequest<BalnearioPublicoResponse>(`/balnearios/${slug}`);
}

export function obtenerBranding(slug: string): Promise<BrandingTokens> {
  return apiRequest<BrandingTokens>(`/balnearios/${slug}/branding`);
}

export function listarUbicaciones(slug: string): Promise<UbicacionResponse[]> {
  return apiRequest<UbicacionResponse[]>(`/balnearios/${slug}/ubicaciones`);
}
