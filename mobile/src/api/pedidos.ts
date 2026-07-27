import {apiRequest} from './client';
import type {PedidoEventoResponse, PedidoResponse} from './types';

export type ItemCarrito = {
  productoId: number;
  productoVarianteId: number | null;
  cantidad: number;
};

/**
 * `idempotencyKey` es OBLIGATORIO (etapa 04/13): la conexión de playa es
 * mala y el reintento (ej. tras "modo avión a mitad de un pedido", criterio
 * de aceptación de esta etapa) tiene que devolver el MISMO pedido, nunca
 * duplicar el cobro. `cardToken` lo genera el SDK de MP en el dispositivo -
 * acá nunca se ve un PAN/CVV real.
 */
export function crearPedido(
  estadiaPublicId: string,
  items: ItemCarrito[],
  cardToken: string,
  idempotencyKey: string,
): Promise<PedidoResponse> {
  return apiRequest<PedidoResponse>('/pedidos', {
    method: 'POST',
    body: {estadiaPublicId, items, cardToken},
    idempotencyKey,
  });
}

export function pedidosDeEstadia(estadiaPublicId: string): Promise<PedidoResponse[]> {
  return apiRequest<PedidoResponse[]>(`/pedidos?estadiaPublicId=${encodeURIComponent(estadiaPublicId)}`);
}

export function obtenerPedido(publicId: string): Promise<PedidoResponse> {
  return apiRequest<PedidoResponse>(`/pedidos/${publicId}`);
}

/** Fallback de polling (ADR-003): todo estado es reconstruible por GET, sin depender del SSE. */
export function historialDePedido(publicId: string): Promise<PedidoEventoResponse[]> {
  return apiRequest<PedidoEventoResponse[]>(`/pedidos/${publicId}/historial`);
}

export function cancelarPedido(publicId: string, motivo?: string): Promise<PedidoResponse> {
  return apiRequest<PedidoResponse>(`/pedidos/${publicId}/cancelacion`, {
    method: 'POST',
    body: motivo ? {motivo} : undefined,
  });
}
