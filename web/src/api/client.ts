import { API_BASE_URL, REQUEST_TIMEOUT_MS } from './config';
import { ApiError } from './ApiError';
import type { ProblemDetail, TokenResponse } from './types';
import { getAccessToken, setAccessToken, notificarSesionPerdida } from '../auth/tokenHolder';
import { guardarRefreshToken, leerRefreshToken, borrarRefreshToken } from '../auth/storage';

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE';

type Opciones = {
  method?: Metodo;
  body?: unknown;
  /** multipart/form-data: FormData ya armado por el caller (fotos, assets de theme). */
  formData?: FormData;
  esRutaDeAuth?: boolean;
  /**
   * Status codes que NO deben lanzar {@link ApiError}: el body se parsea y
   * devuelve igual, tal como vino. Caso real: `PUT /admin/branding` responde
   * 409 con un `BrandingUpdateResult{aplicado:false, ajustesPropuestos}` -
   * no es un error de la request, es una respuesta válida que el caller
   * necesita leer completa (tokens.md: "el guardado exige aceptarlo o
   * corregir").
   */
  statusComoExito?: number[];
};

// Evita que N requests en vuelo disparen N refresh en paralelo (mismo patrón que mobile, etapa 16).
let refreshEnCurso: Promise<string | null> | null = null;

async function fetchConTimeout(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}

async function parsearError(response: Response): Promise<ApiError> {
  try {
    const problem: ProblemDetail = await response.json();
    return new ApiError(problem, response.status);
  } catch {
    return new ApiError(null, response.status);
  }
}

async function refrescarSesion(): Promise<string | null> {
  if (refreshEnCurso) {
    return refreshEnCurso;
  }
  refreshEnCurso = (async () => {
    const refreshToken = leerRefreshToken();
    if (!refreshToken) {
      return null;
    }
    try {
      const response = await fetchConTimeout(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });
      if (!response.ok) {
        borrarRefreshToken();
        return null;
      }
      const tokens: TokenResponse = await response.json();
      setAccessToken(tokens.accessToken);
      guardarRefreshToken(tokens.refreshToken);
      return tokens.accessToken;
    } catch {
      return null;
    }
  })();
  const resultado = await refreshEnCurso;
  refreshEnCurso = null;
  return resultado;
}

export async function apiRequest<T>(path: string, opciones: Opciones = {}): Promise<T> {
  const { method = 'GET', body, formData, esRutaDeAuth = false, statusComoExito = [] } = opciones;

  const ejecutar = async (): Promise<Response> => {
    const headers: Record<string, string> = {};
    if (!formData) {
      headers['Content-Type'] = 'application/json';
    }
    const token = getAccessToken();
    if (token && !esRutaDeAuth) {
      headers.Authorization = `Bearer ${token}`;
    }
    let response: Response;
    try {
      response = await fetchConTimeout(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: formData ?? (body !== undefined ? JSON.stringify(body) : undefined),
      });
    } catch {
      throw new ApiError(null, 0, true);
    }
    return response;
  };

  let response = await ejecutar();

  if (response.status === 401 && !esRutaDeAuth) {
    const nuevoToken = await refrescarSesion();
    if (!nuevoToken) {
      notificarSesionPerdida();
      throw await parsearError(response);
    }
    response = await ejecutar();
  }

  if (!response.ok && !statusComoExito.includes(response.status)) {
    throw await parsearError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}

/** Para endpoints CSV: devuelve el body crudo + el nombre sugerido por Content-Disposition. */
export async function apiDownload(path: string): Promise<{ blob: Blob; filename: string }> {
  const token = getAccessToken();
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetchConTimeout(`${API_BASE_URL}${path}`, { headers });
  if (!response.ok) {
    throw await parsearError(response);
  }
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const match = /filename="?([^"]+)"?/.exec(disposition);
  const filename = match?.[1] ?? 'reporte.csv';
  const blob = await response.blob();
  return { blob, filename };
}
