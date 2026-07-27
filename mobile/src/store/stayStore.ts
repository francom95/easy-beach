import {create} from 'zustand';
import type {EstadiaResponse} from '../api/types';

/**
 * El balneario "en foco" y su estadía en curso. El cliente puede tener
 * estadías vigentes en varios balnearios (etapa 12), pero la UI siempre
 * opera sobre UNA a la vez - la que se eligió en S02 o la que se retomó al
 * reabrir la app (S04, re-ingreso directo a home).
 */
type StayState = {
  balnearioSlug: string | null;
  balnearioNombre: string | null;
  estadia: EstadiaResponse | null;
  entrarABalneario: (slug: string, nombre: string) => void;
  setEstadia: (estadia: EstadiaResponse | null) => void;
  /** Único camino de vuelta a la marca EasyBeach (etapa 06): cerrar/abandonar la estadía. */
  volverASelectorDeBalnearios: () => void;
};

export const useStayStore = create<StayState>(set => ({
  balnearioSlug: null,
  balnearioNombre: null,
  estadia: null,

  entrarABalneario: (slug, nombre) => set({balnearioSlug: slug, balnearioNombre: nombre}),

  setEstadia: estadia => set({estadia}),

  volverASelectorDeBalnearios: () => set({balnearioSlug: null, balnearioNombre: null, estadia: null}),
}));
