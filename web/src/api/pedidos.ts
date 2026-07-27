import { apiRequest } from './client';
import type { EstadoPedido, PedidoEventoResponse, PedidoResponse } from './types';

export function colaPedidos(): Promise<PedidoResponse[]> {
  return apiRequest<PedidoResponse[]>('/operativo/pedidos/cola');
}

export function transicionarPedido(publicId: string, estado: EstadoPedido, motivo?: string): Promise<PedidoResponse> {
  return apiRequest<PedidoResponse>(`/operativo/pedidos/${publicId}/estado`, { method: 'PUT', body: { estado, motivo } });
}

export function cancelarPedido(publicId: string, motivo: string): Promise<PedidoResponse> {
  return apiRequest<PedidoResponse>(`/operativo/pedidos/${publicId}/cancelacion`, {
    method: 'POST',
    body: { estado: 'CANCELADO', motivo },
  });
}

export function historialPedido(publicId: string): Promise<PedidoEventoResponse[]> {
  return apiRequest<PedidoEventoResponse[]>(`/operativo/pedidos/${publicId}/historial`);
}
