import { create } from 'zustand';

export type EstadoAuth = 'cargando' | 'autenticado' | 'anonimo';

type AuthState = {
  estado: EstadoAuth;
  rol: string | null;
  balnearioId: number | null;
  setSesion: (rol: string, balnearioId: number | null) => void;
  cerrarSesion: () => void;
};

export const useAuthStore = create<AuthState>(set => ({
  estado: 'cargando',
  rol: null,
  balnearioId: null,
  setSesion: (rol, balnearioId) => set({ estado: 'autenticado', rol, balnearioId }),
  cerrarSesion: () => set({ estado: 'anonimo', rol: null, balnearioId: null }),
}));
