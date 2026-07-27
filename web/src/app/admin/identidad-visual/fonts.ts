import { Archivo, Baloo_2, Figtree, Lexend, Marcellus, Nunito } from 'next/font/google';

/** Set curado de 4 parejas (tokens.md) — solo se cargan acá, no en el layout raíz (el panel de staff usa su propia tipografía fija). */
export const lexend = Lexend({ subsets: ['latin'], weight: ['400', '700'] });
export const baloo2 = Baloo_2({ subsets: ['latin'], weight: ['700'] });
export const nunito = Nunito({ subsets: ['latin'], weight: ['400', '700'] });
export const marcellus = Marcellus({ subsets: ['latin'], weight: ['400'] });
export const figtree = Figtree({ subsets: ['latin'], weight: ['400', '700'] });
export const archivo = Archivo({ subsets: ['latin'], weight: ['400', '700'] });

export const PAREJAS: Record<string, { display: string; ui: string }> = {
  clara: { display: lexend.style.fontFamily, ui: lexend.style.fontFamily },
  amigable: { display: baloo2.style.fontFamily, ui: nunito.style.fontFamily },
  elegante: { display: marcellus.style.fontFamily, ui: figtree.style.fontFamily },
  energica: { display: archivo.style.fontFamily, ui: archivo.style.fontFamily },
};
