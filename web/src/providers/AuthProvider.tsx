'use client';

import { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useRouter } from 'next/navigation';
import { loginStaff, logout as logoutRequest } from '../api/auth';
import { API_BASE_URL } from '../api/config';
import { getAccessToken, registrarListenerSesionPerdida, setAccessToken } from '../auth/tokenHolder';
import { borrarRefreshToken, guardarRefreshToken, leerRefreshToken } from '../auth/storage';
import { useAuthStore } from '../store/authStore';
import type { TokenResponse } from '../api/types';

type AuthContextValue = {
  login: (email: string, password: string) => Promise<TokenResponse>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

/** Refresco silencioso al montar (recarga de página = access token en memoria perdido, igual que mobile etapa 16). */
async function refrescarAlIniciar(): Promise<TokenResponse | null> {
  const refreshToken = leerRefreshToken();
  if (!refreshToken) return null;
  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) {
      borrarRefreshToken();
      return null;
    }
    return (await response.json()) as TokenResponse;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const setSesion = useAuthStore(s => s.setSesion);
  const cerrarSesion = useAuthStore(s => s.cerrarSesion);
  const [listo, setListo] = useState(false);
  const yaIntento = useRef(false);

  useEffect(() => {
    registrarListenerSesionPerdida(() => {
      cerrarSesion();
      router.replace('/login');
    });
  }, [cerrarSesion, router]);

  useEffect(() => {
    if (yaIntento.current) return;
    yaIntento.current = true;
    // Ambas ramas resuelven setListo(true) en un callback async, nunca de
    // forma síncrona en el cuerpo del efecto (react-hooks/set-state-in-effect):
    // ya hay un access token en memoria (Promise.resolve) o hace falta
    // refrescarlo contra el server (refrescarAlIniciar), da igual el camino.
    const inicializar = getAccessToken() ? Promise.resolve(null) : refrescarAlIniciar();
    inicializar.then(tokens => {
      if (tokens) {
        setAccessToken(tokens.accessToken);
        guardarRefreshToken(tokens.refreshToken);
        setSesion(tokens.rol, tokens.balnearioId);
      } else if (!getAccessToken()) {
        cerrarSesion();
      }
      setListo(true);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const value: AuthContextValue = {
    async login(email, password) {
      const tokens = await loginStaff(email, password);
      setAccessToken(tokens.accessToken);
      guardarRefreshToken(tokens.refreshToken);
      setSesion(tokens.rol, tokens.balnearioId);
      return tokens;
    },
    async logout() {
      const refreshToken = leerRefreshToken();
      setAccessToken(null);
      borrarRefreshToken();
      cerrarSesion();
      if (refreshToken) {
        try {
          await logoutRequest(refreshToken);
        } catch {
          // sesión ya se cerró del lado del cliente igual - el revoke del server es best-effort
        }
      }
      router.replace('/login');
    },
  };

  if (!listo) {
    return null;
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}
