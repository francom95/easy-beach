# Etapa 13 — Backend carrito, pedidos, estados y tiempo real

- **Orden:** 13
- **Modelo ejecutor:** opus
- **Tipo:** construcción
- **Depende de:** 11, 12
- **Estado: ✅ EJECUTADA** — código en [`backend/`](../../backend/) (módulos
  `ordering`, `payments` completo, `promotions` contrato, `shared/realtime`),
  entregable en
  [`docs/especificacion/13-backend-pedidos.md`](../especificacion/13-backend-pedidos.md).
  58/58 tests, BUILD SUCCESS: flujo de punta a punta, idempotencia (un pedido
  y un solo cobro), precios calculados por el servidor, webhook de MP con
  firma verificada e idempotente, cola operativa solo con pago aprobado, SSE
  con fallback por polling, aislamiento cross-tenant.

## Objetivo

Implementar el corazón transaccional de la plataforma: el pedido. Acá se
concreta la venta — el objetivo del negocio. Lo ejecuta opus por la combinación
de máquina de estados, precios congelados, idempotencia, concurrencia y canal
de tiempo real.

## Alcance / Entregables

1. **Creación de pedido desde carrito**:
   - El carrito vive en el cliente (mobile); el backend recibe el pedido
     completo y lo valida server-side: estadía **activa (ya validada por el
     carpero)** del solicitante, productos y **variantes** existentes y
     disponibles, precios recalculados en servidor (nunca confiar en los del
     cliente), promociones aplicadas y verificadas.
   - **Precios e ítems congelados** en el pedido, sobre la variante elegida
     (histórico inmutable, etapa 03).
   - **Idempotencia** por clave de cliente (etapa 04): el reintento por mala
     señal no duplica el pedido ni el cobro. Test específico.
   - El pedido queda ligado a estadía + ubicación de entrega actual.
2. **Pago del pedido (Mercado Pago, Checkout API — decisión de etapa 01)**:
   - Creación del pago contra la **cuenta MP del balneario** (token OAuth de
     la etapa 10), con `application_fee = 0`.
   - Recepción del **webhook** de MP (validado según etapa 05) que resuelve el
     pago: aprobado → el pedido pasa a `CONFIRMADO` y entra a la cola
     operativa; rechazado → `PAGO_RECHAZADO` con reintento de pago posible.
   - Un pedido **no aparece en la cola operativa hasta que su pago está
     aprobado**. Manejo de webhook demorado/duplicado/fuera de orden.
   - Cancelación por el local de un pedido ya pagado → reembolso vía MP
     (registrado en `pedido_pago`).
3. **Máquina de estados completa** (etapa 03): transiciones por rol (el
   operador confirma/prepara/entrega; el cliente solo cancela en estados
   tempranos; cancelación por el local con motivo), registro de cada transición
   en `pedido_evento` (timestamp + actor), validación de transiciones inválidas.
4. **Colas operativas**: pedidos activos (pagados) por balneario ordenados por
   antigüedad, filtrado por estado — el contrato exacto que el panel operativo
   (etapa 17) consume.
5. **Tiempo real** según ADR de la etapa 02: notificación al cliente del cambio
   de estado de su pedido (incluido el resultado del pago) y al panel operativo
   del pedido nuevo; fallback por polling documentado y funcional.
6. **Aplicación de promociones en el pedido**: el cálculo del descuento/combo se
   ejecuta y persiste acá (la definición de promociones llega en la etapa 14;
   esta etapa deja la interfaz de cálculo lista y testeada con una promoción
   de porcentaje simple). El monto cobrado vía MP es el total con promociones
   ya aplicadas.
7. **Totales**: subtotal, descuentos, total; redondeo definido; consistencia
   con el resumen de estadía (etapa 12) y con el monto efectivamente cobrado.

## Inputs requeridos

- Máquina de estados de pedido cerrada (etapa 03) y decisión de canal de tiempo
  real (etapa 02).

## Criterios de aceptación

- Test de idempotencia: misma clave dos veces → un solo pedido y un solo cobro.
- Test de manipulación: pedido con precio adulterado por el cliente → el total
  lo calcula el servidor.
- Test de webhook: notificación duplicada no re-procesa el pago; webhook
  falsificado (firma inválida) es rechazado; un pedido sin pago aprobado no
  entra a la cola operativa.
- Toda transición de estado emite el evento de tiempo real y queda en el
  historial.
- Un operador del balneario A no ve ni transiciona pedidos del B (test).
- El flujo completo crear → confirmar → preparar → entregar corre en un test de
  integración de punta a punta.
