import {API_BASE_URL, REQUEST_TIMEOUT_MS} from './config';
import {ApiError} from './ApiError';
import type {ProblemDetail, TokenResponse} from './types';
import {getAccessToken, setAccessToken, notificarSesionPerdida} from '../auth/tokenHolder';
import {guardarRefreshToken, leerRefreshToken, borrarRefreshToken} from '../auth/secureStorage';

type Metodo = 'GET' | 'POST' | 'PUT' | 'DELETE';

type Opciones = {
  method?: Metodo;
  body?: unknown;
  idempotencyKey?: string;
  /** Para /auth/**: nunca deben reintentar refresh sobre sí mismas (evita loops). */
  esRutaDeAuth?: boolean;
};

// Evita que 10 requests en vuelo disparen 10 refresh en paralelo: todas
// esperan la MISMA promesa de refresh en curso.
let refreshEnCurso: Promise<string | null> | null = null;

async function fetchConTimeout(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, {...init, signal: controller.signal});
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

/** Refresca el access token una sola vez para todos los requests concurrentes que lo disparan. */
async function refrescarSesion(): Promise<string | null> {
  if (refreshEnCurso) {
    return refreshEnCurso;
  }
  refreshEnCurso = (async () => {
    const refreshToken = await leerRefreshToken();
    if (!refreshToken) {
      return null;
    }
    try {
      const response = await fetchConTimeout(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({refreshToken}),
      });
      if (!response.ok) {
        // Etapa 05: reutilización de un refresh ya rotado revoca toda la
        // familia - no hay nada que reintentar, la sesión terminó de verdad.
        await borrarRefreshToken();
        return null;
      }
      const tokens: TokenResponse = await response.json();
      setAccessToken(tokens.accessToken);
      await guardarRefreshToken(tokens.refreshToken);
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
  const {method = 'GET', body, idempotencyKey, esRutaDeAuth = false} = opciones;

  const ejecutar = async (): Promise<Response> => {
    const headers: Record<string, string> = {'Content-Type': 'application/json'};
    const token = getAccessToken();
    if (token && !esRutaDeAuth) {
      headers.Authorization = `Bearer ${token}`;
    }
    if (idempotencyKey) {
      // Nombre exacto de header del contrato (etapa 04 §6): reintentar con la
      // misma clave devuelve el pedido ya creado, nunca duplica el cobro.
      headers['Idempotency-Key'] = idempotencyKey;
    }
    let response: Response;
    try {
      response = await fetchConTimeout(`${API_BASE_URL}${path}`, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined,
      });
    } catch {
      throw new ApiError(null, 0, true);
    }
    return response;
  };

  let response = await ejecutar();

  // 401 fuera de /auth/**: un solo intento de refresh + reintento del
  // request original. Si el refresh también falla, la sesión se dio por
  // terminada de verdad (ver refrescarSesion) y se avisa al authStore.
  if (response.status === 401 && !esRutaDeAuth) {
    const nuevoToken = await refrescarSesion();
    if (!nuevoToken) {
      notificarSesionPerdida();
      throw await parsearError(response);
    }
    response = await ejecutar();
  }

  if (!response.ok) {
    throw await parsearError(response);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  return response.json() as Promise<T>;
}
