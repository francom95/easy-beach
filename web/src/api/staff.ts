import { apiRequest } from './client';
import type { InvitarStaffResponse, MiembroResponse } from './types';

export function listarStaff(): Promise<MiembroResponse[]> {
  return apiRequest<MiembroResponse[]>('/admin/staff');
}

export function invitarStaff(email: string, nombre: string, rol: 'CARPERO' | 'OPERADOR'): Promise<InvitarStaffResponse> {
  return apiRequest<InvitarStaffResponse>('/admin/staff', { method: 'POST', body: { email, nombre, rol } });
}

export function revocarStaff(usuarioPublicId: string): Promise<void> {
  return apiRequest<void>(`/admin/staff/${usuarioPublicId}`, { method: 'DELETE' });
}
