/**
 * Contrato de tokens v1.0 (docs/design/tokens.md). Lo que NO viene del
 * servidor (spacing, radii, sombras, escala tipográfica) es "común a la
 * plataforma": vive en el binario, no en la API - por diseño (legibilidad
 * bajo sol, consistencia entre balnearios).
 */

export const SPACING = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const RADII = {
  control: 10,
  card: 16,
  pill: 999,
} as const;

/** Escala fija en px (etapa 06): legibilidad al sol, no personalizable ni siquiera por el admin. */
export const TYPE_SCALE = {
  display: 30,
  title: 22,
  body: 17,
  label: 14,
  price: 20,
} as const;

export const SHADOW_LEVELS = {
  low: {
    shadowColor: '#000000',
    shadowOffset: {width: 0, height: 1},
    shadowOpacity: 0.08,
    shadowRadius: 3,
    elevation: 2,
  },
  high: {
    shadowColor: '#000000',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.12,
    shadowRadius: 10,
    elevation: 6,
  },
} as const;

export type ParFuente = {nombre: string; regular: string; bold: string};

/**
 * 4 pares curados (etapa 06). Los nombres de familia asumen que los .ttf
 * OFL correspondientes están linkeados en el binario nativo (Android
 * `res/font` / iOS `Info.plist` UIAppFonts) - **eso queda pendiente**, ver
 * el entregable de esta etapa: acá solo se define el contrato de nombres,
 * no se empaquetan los archivos de fuente reales.
 */
export const FONT_PAIRS: Record<string, ParFuente> = {
  clara: {nombre: 'Lexend', regular: 'Lexend-Regular', bold: 'Lexend-Bold'},
  amigable: {nombre: 'Baloo 2 + Nunito', regular: 'Nunito-Regular', bold: 'Baloo2-Bold'},
  elegante: {nombre: 'Marcellus + Figtree', regular: 'Figtree-Regular', bold: 'Marcellus-Regular'},
  energica: {nombre: 'Archivo', regular: 'Archivo-Regular', bold: 'Archivo-Bold'},
};

export const FONT_PAIR_DEFAULT = 'clara';
