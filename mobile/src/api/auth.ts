import {apiRequest} from './client';
import type {TokenResponse} from './types';

export function registrarCliente(email: string, password: string, nombre: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/registro', {
    method: 'POST',
    body: {email, password, nombre},
    esRutaDeAuth: true,
  });
}

export function loginCliente(email: string, password: string): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/login/cliente', {
    method: 'POST',
    body: {email, password},
    esRutaDeAuth: true,
  });
}

export function logout(refreshToken: string): Promise<void> {
  return apiRequest<void>('/auth/logout', {
    method: 'POST',
    body: {refreshToken},
    esRutaDeAuth: true,
  });
}
