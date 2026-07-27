/**
 * Refresh token en {@code localStorage} (no hay Keychain en un navegador):
 * aceptable para un panel interno de staff servido siempre por HTTPS en
 * producción - documentado como adaptación MVP frente al Keychain real de
 * mobile (etapa 16).
 */
const KEY = 'eb_refresh_token';

export function guardarRefreshToken(token: string): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(KEY, token);
}

export function leerRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem(KEY);
}

export function borrarRefreshToken(): void {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(KEY);
}
