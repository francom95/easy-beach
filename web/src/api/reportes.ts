import { apiDownload, apiRequest } from './client';
import type {
  DashboardResumenResponse,
  EstadiasReporteResponse,
  ProductoVendidoResponse,
  PromocionRendimientoResponse,
  ServiciosReporteResponse,
  VentasReporteResponse,
} from './types';

function rango(desde: string, hasta: string): string {
  return `desde=${desde}&hasta=${hasta}`;
}

export function reporteVentas(desde: string, hasta: string): Promise<VentasReporteResponse> {
  return apiRequest<VentasReporteResponse>(`/admin/reportes/ventas?${rango(desde, hasta)}`);
}

export function reporteProductosMasVendidos(desde: string, hasta: string, limite = 10): Promise<ProductoVendidoResponse[]> {
  return apiRequest<ProductoVendidoResponse[]>(`/admin/reportes/productos-mas-vendidos?${rango(desde, hasta)}&limite=${limite}`);
}

export function reportePromociones(desde: string, hasta: string): Promise<PromocionRendimientoResponse[]> {
  return apiRequest<PromocionRendimientoResponse[]>(`/admin/reportes/promociones?${rango(desde, hasta)}`);
}

export function reporteEstadias(desde: string, hasta: string): Promise<EstadiasReporteResponse> {
  return apiRequest<EstadiasReporteResponse>(`/admin/reportes/estadias?${rango(desde, hasta)}`);
}

export function reporteServicios(desde: string, hasta: string): Promise<ServiciosReporteResponse> {
  return apiRequest<ServiciosReporteResponse>(`/admin/reportes/servicios?${rango(desde, hasta)}`);
}

export function reporteDashboard(): Promise<DashboardResumenResponse> {
  return apiRequest<DashboardResumenResponse>('/admin/reportes/dashboard');
}

export type ReporteTipo = 'ventas' | 'productos-mas-vendidos' | 'promociones' | 'estadias' | 'servicios';

export async function descargarCsv(tipo: ReporteTipo, desde: string, hasta: string): Promise<void> {
  const { blob, filename } = await apiDownload(`/admin/reportes/${tipo}/csv?${rango(desde, hasta)}`);
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
