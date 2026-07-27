import {apiRequest} from './client';
import type {MenuCategoriaResponse, PromocionResumen} from './types';

/** El endpoint más consultado de la plataforma (etapa 11): categorías -> productos -> variantes en 1 sola llamada. */
export function obtenerMenu(slug: string): Promise<MenuCategoriaResponse[]> {
  return apiRequest<MenuCategoriaResponse[]>(`/balnearios/${slug}/menu`);
}

/** Sección de promociones (etapa 07 S26), incluye combos que el menú no embebe por producto. */
export function listarPromociones(slug: string): Promise<PromocionResumen[]> {
  return apiRequest<PromocionResumen[]>(`/balnearios/${slug}/promociones`);
}
