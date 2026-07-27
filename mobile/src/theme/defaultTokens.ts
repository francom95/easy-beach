import type {BrandingTokens} from '../api/types';

/**
 * Theme EasyBeach neutro (docs/design/tokens.md defaults). Se usa: (a) antes
 * de elegir balneario (zona 1), (b) si un token puntual falta en la
 * respuesta del servidor (regla del contrato: "faltantes toman default"),
 * y (c) NUNCA dentro de una estadía activa (ahí el fallback es el último
 * theme cacheado, jamás este neutro - ver ThemeProvider).
 */
export const DEFAULT_TOKENS: Required<Omit<BrandingTokens, 'asset.logo' | 'asset.logo-compact' | 'asset.cover' | 'asset.splash' | 'asset.product-placeholder'>> & BrandingTokens = {
  'theme.contract': '1.0',
  'theme.name': 'EasyBeach',
  'color.primary': '#C95100',
  'color.on-primary': '#FFFFFF',
  'color.secondary': '#17437B',
  'color.on-secondary': '#FFFFFF',
  'color.background': '#F5EFE2',
  'color.on-background': '#1A1A1A',
  'color.surface': '#FFFFFF',
  'color.on-surface': '#1A1A1A',
  'color.on-surface-muted': '#6B6B6B',
  'color.border': '#E3DACB',
  'color.success': '#1E7D3C',
  'color.on-success': '#FFFFFF',
  'color.warning': '#B25E00',
  'color.on-warning': '#FFFFFF',
  'color.error': '#C22F2F',
  'color.on-error': '#FFFFFF',
  'color.info': '#1D62B4',
  'color.on-info': '#FFFFFF',
  'typography.family': 'clara',
};
