# Etapa 13 — Backend carrito, pedidos, estados y tiempo real

- **Estado:** ejecutada. Cierra el núcleo transaccional: acá se concreta la
  venta. Insumo de las etapas 14 (promociones reales), 15 (reportes) y 17
  (panel operativo).
- **Corresponde al plan:** [docs/etapas/13-backend-pedidos.md](../etapas/13-backend-pedidos.md)
- **Depende de:** [11](11-backend-catalogo-ubicaciones.md), [12](12-backend-estadia-activa.md).
- **Código:** [`backend/`](../../backend/) — módulos `ordering` (completo),
  `payments` (completado: cobro, webhook, reembolso), `promotions` (contrato),
  `shared/realtime` (SSE).

## 1. Qué se construyó

### Creación de pedido: el servidor es la única autoridad de precios
`CrearPedidoRequest` **no tiene campo de precio ni de total** — solo ids de
producto/variante y cantidad. El servidor lee el catálogo, valida
disponibilidad, aplica la regla "si el producto tiene variantes, elegir una
es obligatorio" (etapa 03 §3.4), calcula y **congela** en `pedido_item` el
nombre y el precio unitario. Un cambio posterior del catálogo no altera el
histórico.

Idempotencia doble: UK `(balneario_id, idempotency_key)` en nuestra base **y**
header `X-Idempotency-Key` hacia MP. Un reintento por mala señal devuelve el
pedido original y **no genera un segundo cobro** (verificado contando los
cobros disparados en el fake).

### Máquina de estados y el invariante central
`EstadoPedido` concentra las transiciones válidas y tres predicados que
gobiernan el comportamiento: `entroACola()`, `estaEnCurso()`,
`tienePagoAprobado()`. **Un pedido no aparece en la cola operativa hasta que
su pago está aprobado** — sin plata confirmada, la cocina no ve nada.

Cada transición queda en `pedido_evento` (estado anterior/nuevo, actor,
tipo de actor, motivo) y emite el evento SSE correspondiente. El cliente solo
puede cancelar en estados tempranos; el local puede cancelar mientras no esté
entregado, exigiendo motivo, y si el pago estaba aprobado dispara el
**reembolso** vía MP automáticamente.

### Pagos (ADR-004)
El pago se crea siempre con el `access_token` **del balneario dueño del
pedido**, con `application_fee = 0` como constante del servidor. El webhook:
verifica la firma HMAC `x-signature`, descarta duplicados por UK
(`payment_id + tipo + hash`), **reconsulta el pago a MP** antes de mover nada
(el body no es fuente de verdad), verifica que el monto coincida con el
registrado, e ignora notificaciones fuera de orden que intentarían degradar
un pago ya aprobado.

### Tiempo real (ADR-003)
SSE con dos canales (`/stream/cliente`, `/stream/operativo`), heartbeat cada
25 s y limpieza de conexiones muertas. Se respeta la regla de diseño del ADR:
**SSE es optimización de latencia, nunca la única vía** — todo estado es
reconstruible por GET (`/pedidos/{id}/historial`, `/operativo/pedidos/cola`).
Una conexión SSE rota jamás rompe la transacción de negocio que la disparó.

### Cierre del círculo con la etapa 12
`ConsumoEstadiaDePedidos` implementa el `ConsumoEstadiaProvider` que la etapa
12 había dejado planteado. Al existir este bean, el
`SinPedidosConsumoProvider` (`@ConditionalOnMissingBean`) desaparece solo,
**sin tocar una línea de `stay`** — exactamente como se había previsto. Ahora
el cierre de estadía se bloquea de verdad con pedidos en curso, y el resumen
de consumo cuenta pedidos entregados reales (ambas cosas verificadas por test).

## 2. Bugs reales encontrados durante la verificación

1. **Orden invertido entre transición y cobro (bug de diseño propio).** El
   pedido se movía a `PAGO_PENDIENTE` *después* de llamar a `iniciarPago`.
   Como MP resuelve sincrónicamente en muchos casos, el evento `PagoResuelto`
   llegaba cuando el pedido todavía estaba en `CREADO`: el listener no
   encontraba nada que confirmar y el pedido quedaba **colgado en
   `PAGO_PENDIENTE` para siempre**. Cuatro tests lo detectaron. **Fix:** mover
   a `PAGO_PENDIENTE` *antes* de cobrar.
2. **Violación de ADR-002: `ordering → identity`.** Los controllers usaban
   `CurrentUserResolver` (de `identity.web`) para traducir ULID → id numérico,
   dependencia que ADR-002 no permite. ArchUnit lo atrapó. **Fix:** agregar el
   claim `uid` al JWT y exponerlo en `EasyBeachUserPrincipal`. Además de
   eliminar la dependencia, **borra un lookup a la base por request** y
   permitió eliminar la clase entera (6 controllers simplificados). No expone
   nada sensible: el token está firmado y ningún endpoint acepta ids numéricos
   ajenos — todo se direcciona por ULID.
3. **`MpWebhookNotificacion` con `balnearioId` sin `@TenantScoped`.** Lo había
   justificado en el Javadoc, pero la regla es absoluta y tenía razón: se marca
   `@TenantScoped` igual que `AuditoriaPlataforma` (etapa 10) — consistencia
   con ADR-001, y el flujo del webhook simplemente no habilita el filtro.
4. **Canal SSE del cliente inalcanzable.** El `StreamController` suscribe por
   `usuarioPublicId` (ULID), pero yo emitía usando el id numérico: las
   notificaciones nunca habrían llegado. Detectado al revisar el código antes
   de compilar. **Fix:** denormalizar `cliente_public_id` en `pedido`
   (consistente con `cliente_id`, que la etapa 03 ya denormaliza), evitando
   además otra dependencia hacia `identity`.

## 3. Decisiones de implementación

- **Evento para el retorno del pago, llamada directa para iniciarlo.**
  `ordering → payments` está permitido, así que iniciar el cobro es una
  llamada directa (necesita respuesta síncrona). Pero el webhook vive en
  `payments` y debe empujar un cambio hacia `ordering`, o sea *contra* la
  flecha: eso se resuelve con el evento `PagoResuelto`, tal como preveía la
  tabla de eventos de la etapa 02.
- **Redondeo:** `HALF_UP` a 2 decimales (ARS), aplicado por línea y en los
  totales. Un descuento nunca puede dejar el total en negativo.
- **`CalculadoraPromociones`** queda como contrato en `promotions` con
  implementación neutra (`SinPromocionesCalculadora`); la etapa 14 la
  reemplaza. Acá no hizo falta invertir la dependencia (a diferencia de
  `ConsumoEstadiaProvider`) porque ADR-002 sí permite `ordering → promotions`.

## 4. Cómo se verificó

**58/58 tests, BUILD SUCCESS**, contra MySQL 8 real:

- `PedidoFlujoIntegrationTest` (13): flujo completo crear→confirmar→preparar
  →en camino→entregar con historial verificado; **idempotencia** (misma clave
  dos veces = un pedido y **un solo cobro**); el total sale del catálogo del
  servidor; producto de otro balneario → 404; pago rechazado **no entra a la
  cola**; cancelación de pedido pagado **dispara reembolso**; cancelar sin
  motivo → 422; transición inválida → 409; pedido en curso **bloquea el cierre
  de la estadía**; resumen de cierre cuadra con lo entregado; **operador de
  otro balneario no ve ni transiciona pedidos ajenos**; no se puede pedir desde
  una estadía no validada.
- `WebhookMercadoPagoIntegrationTest` (5): webhook aprobado confirma y mete en
  cola; **duplicado no re-procesa**; pago desconocido no rompe; sin `data.id`
  → 400; **notificación fuera de orden no degrada un pago ya aprobado**.
- `WebhookSignatureVerifierTest` (5): firma válida aceptada, **falsificada
  rechazada**, firma de otro pago rechazada, header mal formado rechazado.
- Suite completa de las etapas 09–12 en verde: sin regresiones.

## 5. Deuda explícita

- **Job de reconciliación de pagos** (ADR-004): el índice
  (`estado`, `created_at`) y el repositorio (`findByEstadoAndCreatedAtBefore`)
  están listos, pero **el job que consulta a MP los `PENDIENTE` viejos no está
  escrito**. Hasta entonces, un webhook definitivamente perdido deja el pedido
  en `PAGO_PENDIENTE` sin resolución automática. Conviene hacerlo junto con el
  job de cierre por fin de temporada que quedó pendiente de la etapa 12.
- **SSE con múltiples instancias**: el registro de emitters es in-memory
  (válido para instancia única, escala del MVP según ADR-003). Escalar
  horizontalmente exige pub/sub externo detrás de la misma interfaz.
- **`Last-Event-ID`**: el ADR-003 prevé reposición de eventos perdidos en la
  reconexión; hoy la reconexión funciona (nativa de SSE) pero sin buffer de
  reposición — el cliente recupera el estado por GET, que es el fallback
  documentado.
