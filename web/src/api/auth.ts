import { apiRequest } from './client';
import { ApiError } from './ApiError';
import type { TokenResponse } from './types';

function loginStaff(email: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/login/staff', {
    method: 'POST',
    body: { email, password },
    esRutaDeAuth: true,
  });
}

function loginSuperAdmin(email: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/login/super-admin', {
    method: 'POST',
    body: { email, password },
    esRutaDeAuth: true,
  });
}

/**
 * Un solo formulario de login para todo el panel de staff (etapa 17) y el de
 * Super Admin (etapa 18): el backend separa `/login/staff` de
 * `/login/super-admin` por `tipo` de usuario (etapa 05), pero pedirle al
 * usuario que elija de antemano no aporta nada - probamos staff primero
 * (caso común) y recién si la credencial falla probamos super-admin.
 */
export async function loginStaffOSuperAdmin(email: string, password: string): Promise<TokenResponse> {
  try {
    return await loginStaff(email, password);
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) {
      return loginSuperAdmin(email, password);
    }
    throw e;
  }
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
