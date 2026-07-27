import {create} from 'zustand';
import * as authApi from '../api/auth';
import {setAccessToken, setOnSesionPerdida} from '../auth/tokenHolder';
import {guardarRefreshToken, leerRefreshToken, borrarRefreshToken} from '../auth/secureStorage';
import {apiRequest} from '../api/client';
import type {TokenResponse} from '../api/types';

type EstadoSesion = 'cargando' | 'autenticado' | 'anonimo';

type AuthState = {
  estado: EstadoSesion;
  refreshTokenActual: string | null;
  login: (email: string, password: string) => Promise<void>;
  registrar: (email: string, password: string, nombre: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Se llama una vez al arrancar la app: intenta recuperar la sesión de temporada (etapa 05: 60 días). */
  restaurarSesion: () => Promise<void>;
};

function aplicarTokens(tokens: TokenResponse): Promise<void> {
  setAccessToken(tokens.accessToken);
  return guardarRefreshToken(tokens.refreshToken);
}

export const useAuthStore = create<AuthState>((set, get) => ({
  estado: 'cargando',
  refreshTokenActual: null,

  login: async (email, password) => {
    const tokens = await authApi.loginCliente(email, password);
    await aplicarTokens(tokens);
    set({estado: 'autenticado', refreshTokenActual: tokens.refreshToken});
  },

  registrar: async (email, password, nombre) => {
    const tokens = await authApi.registrarCliente(email, password, nombre);
    await aplicarTokens(tokens);
    set({estado: 'autenticado', refreshTokenActual: tokens.refreshToken});
  },

  logout: async () => {
    const {refreshTokenActual} = get();
    try {
      if (refreshTokenActual) {
        await authApi.logout(refreshTokenActual);
      }
    } catch {
      // Si el logout server-side falla (ej. sin red), igual se limpia todo
      // localmente - no tiene sentido dejar al usuario "atrapado" logueado.
    }
    setAccessToken(null);
    await borrarRefreshToken();
    set({estado: 'anonimo', refreshTokenActual: null});
  },

  restaurarSesion: async () => {
    const refreshToken = await leerRefreshToken();
    if (!refreshToken) {
      set({estado: 'anonimo'});
      return;
    }
    try {
      const tokens = await apiRequest<TokenResponse>('/auth/refresh', {
        method: 'POST',
        body: {refreshToken},
        esRutaDeAuth: true,
      });
      await aplicarTokens(tokens);
      set({estado: 'autenticado', refreshTokenActual: tokens.refreshToken});
    } catch {
      // Refresh token vencido/revocado (ej. reutilización detectada, etapa 05):
      // no hay sesión que restaurar, vuelve a S01.
      await borrarRefreshToken();
      set({estado: 'anonimo', refreshTokenActual: null});
    }
  },
}));

// El cliente HTTP (api/client.ts) avisa acá cuando un refresh en medio de un
// request cualquiera falla - single source of truth de "la sesión terminó".
setOnSesionPerdida(() => {
  useAuthStore.setState({estado: 'anonimo', refreshTokenActual: null});
});
