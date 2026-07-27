import { apiRequest } from './client';
import type { TokenResponse } from './types';

export function loginStaff(email: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/login/staff', {
    method: 'POST',
    body: { email, password },
    esRutaDeAuth: true,
  });
}

export function cambiarPassword(passwordActual: string, passwordNueva: string): Promise<void> {
  return apiRequest<void>('/auth/cambiar-password', {
    method: 'PUT',
    body: { passwordActual, passwordNueva },
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiRequest<void>('/auth/logout', {
    method: 'POST',
    body: { refreshToken },
    esRutaDeAuth: true,
  });
}
