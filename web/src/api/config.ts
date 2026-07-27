/** El panel corre en el navegador del propio staff: sin el alias 10.0.2.2 de mobile (etapa 16), localhost directo alcanza. */
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/** Origen sin `/api/v1`: los fotoUrl/asset URLs que devuelve el backend son paths relativos a este origen, no a API_BASE_URL. */
export const API_ORIGIN = API_BASE_URL.replace(/\/api\/v1\/?$/, '');

export const REQUEST_TIMEOUT_MS = 20000;

export function assetUrl(path: string): string {
  return path.startsWith('http') ? path : `${API_ORIGIN}${path}`;
}
