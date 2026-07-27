/** Access token SOLO en memoria (nunca localStorage): mismo criterio que mobile (etapa 16) - vive en un módulo, se pierde en cada recarga de página y se recupera con un refresh silencioso al montar la app. */
let accessToken: string | null = null;
let onSesionPerdida: (() => void) | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function registrarListenerSesionPerdida(listener: () => void): void {
  onSesionPerdida = listener;
}

export function notificarSesionPerdida(): void {
  accessToken = null;
  onSesionPerdida?.();
}
