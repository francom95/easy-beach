import { apiRequest } from './client';
import type { BrandingTokens, BrandingUpdateResult } from './types';

export type TypographyFamily = 'clara' | 'amigable' | 'elegante' | 'energica';
export type AssetType = 'LOGO' | 'LOGO_COMPACT' | 'COVER' | 'SPLASH' | 'PRODUCT_PLACEHOLDER';

export type BrandingUpdateInput = {
  themeName: string;
  colorPrimary: string;
  colorSecondary: string;
  colorBackground: string;
  colorSurface: string;
  colorSuccess: string;
  colorWarning: string;
  colorError: string;
  colorInfo: string;
  typographyFamily: TypographyFamily;
  aceptarSugerencia: boolean;
};

export function obtenerBrandingPropio(): Promise<BrandingTokens> {
  return apiRequest<BrandingTokens>('/admin/branding');
}

export function obtenerBrandingPublico(slug: string): Promise<BrandingTokens> {
  return apiRequest<BrandingTokens>(`/balnearios/${slug}/branding`);
}

/**
 * 409 no es un error acá: es una respuesta válida con `aplicado:false` +
 * `ajustesPropuestos` cuando algún color no cumple contraste (tokens.md:
 * "el guardado exige aceptarlo o corregir") - el caller la lee como
 * resultado normal, no como excepción.
 */
export function actualizarBranding(input: BrandingUpdateInput): Promise<BrandingUpdateResult> {
  return apiRequest<BrandingUpdateResult>('/admin/branding', { method: 'PUT', body: input, statusComoExito: [409] });
}

export function subirAssetBranding(tipo: AssetType, file: File): Promise<BrandingTokens> {
  const formData = new FormData();
  formData.append('file', file);
  return apiRequest<BrandingTokens>(`/admin/branding/assets/${tipo}`, { method: 'POST', formData });
}
