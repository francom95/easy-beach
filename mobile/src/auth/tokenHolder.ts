/**
 * Access token en memoria, nunca persistido (TTL corto, 60 min - etapa 05
 * §1). Vive separado de `authStore` para que `api/client.ts` pueda leerlo
 * sin crear una dependencia circular cliente-HTTP <-> store.
 */
let accessToken: string | null = null;
let onSesionPerdida: (() => void) | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function getAccessToken(): string | null {
  return accessToken;
}

/** El authStore se registra acá para enterarse cuando el cliente HTTP no pudo refrescar la sesión. */
export function setOnSesionPerdida(callback: (() => void) | null): void {
  onSesionPerdida = callback;
}

export function notificarSesionPerdida(): void {
  accessToken = null;
  onSesionPerdida?.();
}
