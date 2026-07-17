# Etapa 03 — Modelo de datos (MySQL)

- **Orden:** 03
- **Modelo ejecutor:** opus
- **Tipo:** especificación (documento, sin código)
- **Depende de:** 01, 02
- **Estado: ✅ EJECUTADA** — entregable en
  [`docs/especificacion/03-modelo-de-datos.md`](../especificacion/03-modelo-de-datos.md):
  ER completo (Mermaid), diccionario de datos por tabla, máquinas de estado
  (estadía, pedido, pago, servicio, suscripción), estrategia de índices,
  matriz feature→tablas y plan de migraciones Flyway. Decisiones clave:
  PK `BIGINT` + `public_id` ULID en recursos expuestos; unicidad de estadía
  activa por `(balneario_id, activa_uk)` a nivel DB; precio congelado por
  variante en `pedido_item`; tokens OAuth de MP cifrados en
  `balneario_mp_credencial`.

## Objetivo

Diseñar el esquema relacional completo del MVP, coherente con la estrategia
multitenant de la etapa 02. El modelo de datos es la base de todo el backend:
errores acá se pagan en todas las etapas siguientes.

## Alcance / Entregables

1. **Diagrama entidad-relación** (Mermaid `erDiagram` dentro del md) cubriendo
   como mínimo:
   - **Tenancy/plataforma**: `balneario`, `configuracion_visual` (colores, logo,
     assets), `plan`, `temporada`, `suscripcion_temporada` (estado del balneario
     por temporada).
   - **Identidad**: `usuario`, `rol`, relación usuario–balneario (el staff
     pertenece a un balneario; el cliente es global y se vincula por estadía).
   - **Operación física**: `ubicacion` (tipo: carpa/sombrilla/mesa/sector,
     identificador visible, estado).
   - **Catálogo**: `categoria_menu`, `producto` (precio, foto, disponible,
     orden), y **variantes/opciones** (confirmado en etapa 01: variantes
     simples de un nivel — ej. tamaño o sabor — con ajuste de precio propio;
     entidad `producto_variante`).
   - **Estadía**: `estadia` (cliente, balneario, ubicación, fecha apertura,
     fecha cierre, estado). Regla (etapa 01): única estadía activa **por
     cliente y por balneario** (constraint sobre `cliente_id + balneario_id`,
     no solo `cliente_id`). La apertura es validada por el carpero: incluir el
     estado `PENDIENTE_VALIDACION` previo a `ACTIVA` y el actor que validó.
   - **Pedidos**: `pedido`, `pedido_item` (precio congelado al momento del
     pedido, sobre la **variante elegida**, no el producto base),
     `pedido_evento` (historial de cambios de estado con timestamp y actor).
   - **Pagos** (etapa 01: Mercado Pago marketplace): `pedido_pago` con estado
     (`PENDIENTE`, `APROBADO`, `RECHAZADO`, `REEMBOLSADO`), referencias
     externas de MP (`preference_id`/`payment_id`) y trazabilidad del webhook;
     credenciales OAuth por balneario (`balneario_mp_credencial`) con
     almacenamiento cifrado de tokens.
   - **Servicios al carpero**: `solicitud_servicio` (tipo, estado, ubicación).
   - **Promociones**: `promocion` (tipo básico: % descuento, combo, happy hour
     por franja horaria), vigencia, productos alcanzados.
2. **Diccionario de datos**: por tabla, columnas con tipo MySQL, nullabilidad,
   default, y **reglas de negocio** asociadas (ej.: "no se puede cerrar una
   estadía con pedidos en curso").
3. **Máquinas de estado documentadas**: pedido (ej. `CREADO →
   PAGO_PENDIENTE → CONFIRMADO → EN_PREPARACION → EN_CAMINO → ENTREGADO`, con
   `CANCELADO`/`PAGO_RECHAZADO` y sus reglas — un pedido no se confirma
   operativamente hasta que el webhook de MP marca el pago aprobado), pago,
   solicitud de servicio, estadía (incluyendo `PENDIENTE_VALIDACION` →
   `ACTIVA` validada por carpero), suscripción de temporada. Transiciones
   permitidas y quién puede ejecutarlas.
4. **Estrategia de índices** inicial: por cada consulta caliente prevista
   (pedidos activos por balneario, menú por balneario, estadía activa por
   cliente), el índice que la sirve. Todo índice arranca con `balneario_id`
   donde aplique.
5. **Convenciones**: nombres en singular/plural (definir una y sostenerla),
   claves primarias (recomendar y justificar: auto-increment vs UUID),
   `created_at`/`updated_at` en todas las tablas, soft-delete donde la etapa 02
   lo indicó.
6. **Plan de migraciones**: herramienta (Flyway o Liquibase — decidir) y
   numeración.

## Inputs requeridos

- ADR de multitenancy (etapa 02) — define si toda tabla lleva `balneario_id`.
- Entregable de la etapa 01 (`docs/especificacion/01-vision-alcance-glosario.md`):
  ya resuelve variantes (sí, un nivel), pagos (Mercado Pago marketplace, pago
  por pedido, sin cuenta corriente), unicidad de estadía (por cliente y
  balneario) y validación por carpero.

## Criterios de aceptación

- Cada feature del alcance MVP (etapa 01) tiene sus entidades cubiertas; se
  incluye una matriz feature → tablas.
- Toda tabla tenant-scoped tiene `balneario_id` e índice compuesto que arranca
  por él.
- Las máquinas de estado no tienen transiciones ambiguas ni estados huérfanos.
- El precio de los ítems de pedido queda congelado (histórico) sobre la
  variante elegida, nunca referencia al precio vigente del producto.
- Todo pedido con pago tiene su `pedido_pago` trazable contra las referencias
  de Mercado Pago; los tokens OAuth de balneario nunca se guardan en claro.
