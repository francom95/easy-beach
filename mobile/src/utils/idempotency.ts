/**
 * Genera una clave de idempotencia estable por intento de compra (etapa 13):
 * se crea UNA vez al entrar al checkout y se reutiliza en cada reintento del
 * MISMO pedido (ej. tras "modo avión a mitad de un pedido" - la app
 * reconecta y reenvía con la misma clave, nunca duplica el cobro). Un ULID
 * simplificado alcanza - no hace falta el generador exacto del backend.
 */
export function generarIdempotencyKey(): string {
  const timestamp = Date.now().toString(36);
  const azar = Math.random().toString(36).slice(2, 12);
  return `${timestamp}-${azar}`;
}
