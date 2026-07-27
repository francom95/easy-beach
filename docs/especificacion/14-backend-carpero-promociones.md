# Etapa 14 — Backend servicios al carpero y promociones

- **Estado:** ejecutada. Completa la superficie funcional del MVP backend.
  Insumo de la etapa 15 (reportes) y 16/17 (apps).
- **Corresponde al plan:** [docs/etapas/14-backend-carpero-promociones.md](../etapas/14-backend-carpero-promociones.md)
- **Depende de:** [12](12-backend-estadia-activa.md), [13](13-backend-pedidos.md).
- **Código:** [`backend/`](../../backend/) — módulos `concierge` (completo),
  `promotions` (completo, sobre el contrato de la etapa 13).

## 1. Decisión de negocio cerrada antes de codificar

El plan marcaba la regla de combinación de promociones como "propuesta a
confirmar" (la etapa 03 proponía "no se acumulan, gana la mejor"). Se
confirmó lo contrario: **las promociones se acumulan** — todas las que
apliquen a una línea suman su descuento, sin elegir una sola. Cada
promoción aporta su propia fila en `pedido_promocion`, así el ticket
muestra de dónde sale cada descuento (verificado con un test que aplica un
10% de categoría + 15% de happy hour sobre el mismo producto: el pedido
recibe los **250** de descuento combinados, no 150).

## 2. Servicios al carpero

Ciclo simple sin dinero (etapa 01: cobro de servicios queda fuera del MVP).
`PENDIENTE → EN_CURSO → RESUELTA`, más `CANCELADA`. Misma mecánica de tiempo
real que los pedidos (etapa 13): cola operativa + SSE a ambos canales.

El endpoint de tipos de servicio del cliente **no resuelve por slug**:
resuelve el balneario desde la propia estadía del cliente (vía `stay`, que
`concierge` ya puede usar por ADR-002). Esto no es solo una decisión
arquitectónica — de paso da el ownership-check gratis: un cliente solo ve
tipos de servicio de balnearios donde tiene una estadía real.

## 3. Promociones

Tres tipos del MVP: `DESCUENTO_PORCENTUAL` y `HAPPY_HOUR` (alcance por
producto o categoría), `COMBO` (N productos a precio fijo). El cálculo:

- **Vigencia con TZ correcta** (`VigenciaPromocionChecker`): fechas
  inclusivas en los bordes, franja horaria con soporte para cruzar
  medianoche (ej. 22:00–02:00), día de la semana solo restringe
  `HAPPY_HOUR` (los campos son "Happy hour" por diseño, etapa 03 §3.9). TZ
  `America/Argentina/Buenos_Aires` (etapa 02/04), no UTC.
- **Combo**: se calcula cuántas veces "cabe" la combinación en el pedido
  (mínimo entre todos sus ítems) contra el precio promedio ponderado del
  producto en esa orden — soporta que el mismo producto aparezca en más de
  una línea. Si el combo no es más barato que el precio normal, no aplica
  descuento (protege contra una configuración accidental del admin).
- **El descuento queda congelado** en `pedido_promocion`: una promo vencida
  o desactivada después no altera pedidos históricos (verificado
  explícitamente: se crea un pedido con 20% activo, se desactiva la promo,
  y el pedido histórico sigue mostrando sus 200 de descuento mientras uno
  nuevo ya no recibe nada).

### El punto arquitectónico: otra inversión de dependencia

El menú público (`catalog`, etapa 11) necesita mostrar promociones
vigentes, pero ADR-002 fija `promotions → catalog`, nunca al revés. Mismo
patrón que `ConsumoEstadiaProvider` (etapa 12): la interfaz
(`PromocionesPublicasProvider`) vive en `catalog`, la implementación real la
aporta `promotions`. Mientras tanto rige `SinPromocionesPublicasProvider`
(`@ConditionalOnMissingBean`), que responde "ninguna vigente" — correcto,
no un stub que miente. Los combos no se embeben por producto (aplican a una
combinación, no a un ítem); tienen su propio endpoint público de sección de
promociones.

## 4. Bugs reales encontrados durante la verificación

1. **`pedido_promocion` no insertaba** (bug real de mapeo JPA). `Pedido.
   promociones` era un `@OneToMany` unidireccional con `@JoinColumn`: eso
   hace que Hibernate inserte primero la fila **sin** el FK y la actualice
   después — y falla porque `pedido_id` es `NOT NULL`. Nunca se había
   detectado en la etapa 13 porque `SinPromocionesCalculadora` devolvía
   siempre una lista vacía, así que la lista de promociones del pedido
   jamás se ejercitaba. **Fix:** bidireccional con `@ManyToOne` en
   `PedidoPromocion` + `mappedBy` en `Pedido` — el mismo patrón que ya usaba
   `PedidoItem`, que sí insertaba bien.
2. **Violación de ADR-002 (`concierge → platform`)**, atrapada por ArchUnit.
   El primer diseño del endpoint de tipos de servicio resolvía el balneario
   por slug (como el menú público), pero `concierge` no tiene `platform`
   entre sus dependencias permitidas. **Fix:** en vez de tocar el ADR,
   resolver el balneario desde la propia estadía del cliente — no solo
   respeta el límite de módulos, da un diseño mejor (ver §2).
3. **`TenantContextMissingException` al corregir el bug anterior.** Al
   reescribir el endpoint para resolver por estadía en vez de por slug, se
   perdió el `TenantContext.set(balnearioId)` que la versión anterior sí
   tenía. Un cliente no lleva `balnearioId` en su token (etapa 05 §1.2:
   puede tener estadías en varios balnearios), así que sin ese `set`
   explícito, `TenantFilterService.applyCurrentTenant()` no tiene tenant
   ambiente y lanza. Detectado por el primer test que ejercitó el endpoint.

## 5. Cómo se verificó

**87/87 tests, BUILD SUCCESS**, contra MySQL 8 real:

- `VigenciaPromocionCheckerTest` (10, unitario): bordes de fecha inclusivos,
  franja horaria (incluida la que cruza medianoche), restricción por día
  de la semana solo para happy hour.
- `PromocionCalculoIntegrationTest` (7): descuento % sobre categoría, combo
  con cantidad suficiente, combo que no completa (sin descuento), **dos
  promociones acumulándose** sobre el mismo producto, promo vencida fuera
  del menú y sin aplicar, **desactivar una promo no toca el pedido
  histórico pero sí bloquea los nuevos**, sección pública de promociones
  (incluye combos).
- `PromocionAbmIntegrationTest` (6): validaciones (descuento sin alcance,
  combo con menos de 2 productos, fechas invertidas, producto ajeno como
  alcance → 404), ABM completo, aislamiento cross-tenant.
- `SolicitudServicioFlujoIntegrationTest` (6): listado de tipos por estadía
  propia, flujo completo pendiente→en curso→resuelta con salida de la cola,
  cancelación por el cliente, estadía no activa rechazada, aislamiento
  cross-tenant (un carpero de otro balneario no ve ni transiciona
  solicitudes ajenas).
- Suite completa de las etapas 09–13 en verde: sin regresiones.

## 6. Deuda explícita

Ninguna nueva. La deuda de la etapa 13 (job de reconciliación de pagos, SSE
in-memory de instancia única) sigue igual — no la toca esta etapa.
