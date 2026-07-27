import { apiDownload, apiRequest } from './client';
import type {
  AuditoriaResponse,
  BalnearioResponse,
  CrearBalnearioResponse,
  EstadoSuscripcion,
  EstadoTemporada,
  PageResponse,
  PlanResponse,
  PlataformaReporteResponse,
  SuscripcionResponse,
  TemporadaResponse,
} from './types';

// ---------------------------------------------------------------- balnearios

export function listarBalnearios(page = 0, size = 50): Promise<PageResponse<BalnearioResponse>> {
  return apiRequest<PageResponse<BalnearioResponse>>(`/super-admin/balnearios?page=${page}&size=${size}`);
}

export function obtenerBalneario(id: number): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>(`/super-admin/balnearios/${id}`);
}

export function crearBalneario(input: {
  slug: string;
  nombre: string;
  emailContactoBalneario: string;
  telefono: string;
  nombreAdmin: string;
  emailAdmin: string;
}): Promise<CrearBalnearioResponse> {
  return apiRequest<CrearBalnearioResponse>('/super-admin/balnearios', { method: 'POST', body: input });
}

export function actualizarBalneario(
  id: number,
  input: { nombre: string; emailContacto: string; telefono: string },
): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>(`/super-admin/balnearios/${id}`, { method: 'PUT', body: input });
}

export function activarBalneario(id: number, motivo: string): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>(`/super-admin/balnearios/${id}/activar`, { method: 'POST', body: { motivo } });
}

export function suspenderBalneario(id: number, motivo: string): Promise<BalnearioResponse> {
  return apiRequest<BalnearioResponse>(`/super-admin/balnearios/${id}/suspender`, { method: 'POST', body: { motivo } });
}

// ---------------------------------------------------------------- planes

export function listarPlanes(): Promise<PlanResponse[]> {
  return apiRequest<PlanResponse[]>('/super-admin/planes');
}

export function crearPlan(input: { nombre: string; descripcion: string; precio: string; activo: boolean }): Promise<PlanResponse> {
  return apiRequest<PlanResponse>('/super-admin/planes', { method: 'POST', body: input });
}

export function actualizarPlan(
  id: number,
  input: { nombre: string; descripcion: string; precio: string; activo: boolean },
): Promise<PlanResponse> {
  return apiRequest<PlanResponse>(`/super-admin/planes/${id}`, { method: 'PUT', body: input });
}

// ---------------------------------------------------------------- temporadas

export function listarTemporadas(): Promise<TemporadaResponse[]> {
  return apiRequest<TemporadaResponse[]>('/super-admin/temporadas');
}

export function crearTemporada(input: { nombre: string; fechaInicio: string; fechaFin: string }): Promise<TemporadaResponse> {
  return apiRequest<TemporadaResponse>('/super-admin/temporadas', { method: 'POST', body: input });
}

export function cambiarEstadoTemporada(id: number, estado: EstadoTemporada): Promise<TemporadaResponse> {
  return apiRequest<TemporadaResponse>(`/super-admin/temporadas/${id}/estado`, { method: 'PUT', body: { estado } });
}

// ---------------------------------------------------------------- suscripciones

export function listarSuscripciones(balnearioId: number): Promise<SuscripcionResponse[]> {
  return apiRequest<SuscripcionResponse[]>(`/super-admin/balnearios/${balnearioId}/suscripciones`);
}

export function suscribirBalneario(balnearioId: number, planId: number, temporadaId: number): Promise<SuscripcionResponse> {
  return apiRequest<SuscripcionResponse>(`/super-admin/balnearios/${balnearioId}/suscripciones`, {
    method: 'POST',
    body: { planId, temporadaId },
  });
}

export function cambiarEstadoSuscripcion(
  balnearioId: number,
  suscripcionId: number,
  estado: EstadoSuscripcion,
  motivo?: string,
): Promise<SuscripcionResponse> {
  return apiRequest<SuscripcionResponse>(`/super-admin/balnearios/${balnearioId}/suscripciones/${suscripcionId}/estado`, {
    method: 'PUT',
    body: { estado, motivo },
  });
}

// ---------------------------------------------------------------- auditoría

export function listarAuditoria(balnearioId: number | null, page = 0, size = 20): Promise<PageResponse<AuditoriaResponse>> {
  const filtro = balnearioId ? `&balnearioId=${balnearioId}` : '';
  return apiRequest<PageResponse<AuditoriaResponse>>(`/super-admin/auditoria?page=${page}&size=${size}${filtro}`);
}

// ---------------------------------------------------------------- reporte de plataforma

export function reportePlataforma(): Promise<PlataformaReporteResponse> {
  return apiRequest<PlataformaReporteResponse>('/super-admin/reportes/plataforma');
}

export async function descargarCsvPlataforma(): Promise<void> {
  const { blob, filename } = await apiDownload('/super-admin/reportes/plataforma/csv');
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
