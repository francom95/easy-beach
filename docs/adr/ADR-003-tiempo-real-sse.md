# ADR-003 — Tiempo real: SSE con fallback por polling

- **Estado:** Aceptada
- **Fecha:** 2026-07-07
- **Etapa:** 02

## Contexto

Dos superficies necesitan datos "en vivo":
1. **Cliente (mobile):** estado de su pedido (pagado → confirmado → en
   preparación → en camino), resultado del pago, validación de su estadía.
2. **Panel operativo (tablet):** pedidos nuevos pagados, solicitudes de
   servicio, solicitudes de apertura de estadía.

Condiciones de borde: conectividad de playa mala e intermitente (el canal se
va a caer seguido), y toda la comunicación cliente→servidor ya viaja por REST
(no necesitamos canal bidireccional).

## Opciones evaluadas

**(a) Polling puro.** Simple y robusto, pero con 5–15 k clientes en pico el
costo por request × frecuencia es alto, y la latencia percibida (10–30 s) es
mala justo en el momento más sensible ("¿me cobraron?", "¿viene mi pedido?").

**(b) WebSocket/STOMP.** Bidireccional, bien soportado por Spring. Pero la
bidireccionalidad no se usa (los comandos van por REST), agrega handshake y
manejo de sesión propios, y su reconexión hay que construirla a mano.

**(c) SSE (Server-Sent Events).** Unidireccional servidor→cliente sobre HTTP
plano. Reconexión **automática con `Last-Event-ID`** nativa del protocolo —
exactamente lo que la conectividad de playa necesita. Menos infraestructura
que WebSocket (sin upgrade de protocolo, atraviesa proxies HTTP normales).

## Decisión

**SSE como canal principal + polling como fallback obligatorio.**

Contrato (detalle fino en etapa 04):
- `GET /api/v1/stream/cliente` — eventos del cliente autenticado:
  `estadia.validada`, `pago.resultado`, `pedido.estado`, `servicio.estado`.
- `GET /api/v1/stream/operativo` — eventos del balneario del staff autenticado:
  `pedido.nuevo`, `servicio.nuevo`, `estadia.solicitud`.
- Heartbeat (comentario SSE) cada ~25 s para detectar conexiones muertas;
  `Last-Event-ID` + buffer corto en servidor para reponer eventos perdidos en
  la reconexión.
- **Fallback:** los mismos datos siempre consultables por REST (colas y
  estados); el cliente hace polling adaptativo (ej. cada 15 s) cuando el
  stream no está disponible. Regla de diseño: **SSE es una optimización de
  latencia, nunca la única vía** — todo estado es reconstruible por GET.

Implementación MVP: `SseEmitter` de Spring alimentado por los eventos de
dominio del ADR-002, registro de emitters in-memory (válido para instancia
única). Evolución a múltiples instancias: pub/sub externo (Redis) detrás de la
misma interfaz — documentado, no construido.

## Consecuencias

- Latencia percibida ~instantánea en los dos momentos que más importan (pago
  aprobado, pedido nuevo en el panel) sin pagar la complejidad de WebSocket.
- La app mobile necesita una lib SSE para React Native (ej. `react-native-sse`)
  — validar en la etapa 16; si diera problemas, el fallback por polling ya es
  parte del contrato, no un plan B improvisado.
- El buffer de reposición por `Last-Event-ID` es acotado (ej. últimos 5 min);
  una desconexión más larga se resuelve por el GET de estado, que es la fuente
  de verdad.
