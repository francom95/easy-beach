# Etapa 04 — Convenciones y contratos de API REST

- **Estado:** ejecutada — insumo directo de las etapas 05 (seguridad), 09
  (backend fundacional), 16 (mobile) y 17/18 (web).
- **Corresponde al plan:** [docs/etapas/04-contratos-api.md](../etapas/04-contratos-api.md)
- **Entregables:**
  - Este documento (convenciones normativas + matrices).
  - **Especificación OpenAPI 3.0.3:** [`docs/api/openapi.yaml`](../api/openapi.yaml)
    — 55 endpoints, 59 schemas, validada con `openapi-spec-validator`.
- **Depende de:** [02 (arquitectura)](02-arquitectura-general.md),
  [03 (modelo de datos)](03-modelo-de-datos.md) y ADRs 001/003/004/005.

---

## 1. Convenciones generales (normativas)

### 1.1 Versionado y recursos
- Prefijo **`/api/v1`**. Cambios incompatibles ⇒ `/api/v2`; `/v1` no se rompe
  en plena temporada.
- Recursos en **plural** (`/pedidos`, `/estadias`); sub-recursos anidados solo
  un nivel (`/estadias/{id}/pedidos`). Acciones que no son CRUD se modelan como
  sub-recurso sustantivo + `POST` (`/pedidos/{id}/cancelacion`,
  `/estadias/{id}/validacion`), no verbos en la URL.

### 1.2 Métodos y códigos de estado
| Situación | Código |
|---|---|
| GET/PATCH/PUT ok | 200 |
| POST crea recurso | 201 |
| POST acción aceptada, resultado asíncrono (pago) | 202 |
| Borrado / logout ok, sin cuerpo | 204 |
| Datos inválidos | 422 |
| No autenticado | 401 |
| Autenticado sin permiso (rol/tenant ajeno) | 403 |
| Recurso inexistente o de otro tenant | 404 |
| Conflicto de estado (idempotencia, unicidad, transición inválida) | 409 |
| Rate limit | 429 |

### 1.3 Formato de error — RFC 7807
`Content-Type: application/problem+json`. Campos: `type`, `title`, `status`,
`detail`, `instance`, más un **`code` de negocio estable** (ej.
`ESTADIA_DUPLICADA`, `TRANSICION_INVALIDA`, `PAGO_NO_PERMITIDO`) que mobile/web
usan para lógica y traducción, y `errors[]` para validaciones por campo. El
`code` es contrato; el `title`/`detail` son legibles y pueden cambiar.

### 1.4 Fechas y montos
- **Fechas:** ISO 8601 con offset (`2026-07-16T14:30:00-03:00`). Almacenadas en
  UTC; la TZ de negocio es `America/Argentina/Buenos_Aires` y se aplica en
  filtros de reporte.
- **Montos:** **string decimal** (`"1500.00"`), moneda ARS implícita. Se
  descarta float (errores de redondeo) y minor units (menos legible para un
  dominio de precios "redondos" en pesos). El servidor es la autoridad del
  cálculo; los montos que envíe el cliente se ignoran.

### 1.5 Paginación y filtros
- Paginación **offset**: `?page` (0-based), `?size` (máx. 100). Respuesta
  envuelta con `page/size/totalElements/totalPages`. Suficiente al volumen del
  MVP; se revisa si algún listado crece (etapa 15).
- Colas operativas y "mis estadías" no se paginan (conjuntos chicos por
  balneario/cliente); se ordenan por antigüedad en servidor.

### 1.6 Resolución del tenant (ADR-001) — regla de contrato
| Tipo de request | De dónde sale `balneario_id` |
|---|---|
| Público (menú, branding, listado) | **Path explícito** (`/balnearios/{id\|slug}/...`) |
| Staff (operativo, admin) | **Claim del JWT**; jamás de la URL ni del body |
| Cliente sobre estadía/pedido | Del **recurso validado en servidor** (la estadía conoce su balneario) |
| Super Admin | Contexto cross-tenant explícito y auditado |
- **Ningún endpoint tenant-scoped opera sin tenant resuelto.** El
  `balneario_id` nunca se acepta desde un campo libre del cliente.

### 1.7 Idempotencia
- **`POST /estadias/{id}/pedidos` exige header `Idempotency-Key`.** Misma clave
  + mismo payload ⇒ se devuelve el pedido ya creado (no se duplica pedido ni
  cobro). Misma clave + payload distinto ⇒ `409`. La clave es única por cliente
  (persistida en `pedido`, UK `balneario_id + idempotency_key`).
- **Webhook de MP** idempotente por `payment_id` (tabla
  `mp_webhook_notificacion`): notificación repetida no re-procesa.

### 1.8 Autenticación
- `Authorization: Bearer <JWT>` (access token corto) + refresh token (detalle
  de duración/rotación/revocación en etapa 05). Endpoints públicos: `security: []`.

---

## 2. Comportamiento del flujo crítico "crear pedido"

Cubre el criterio de aceptación de la etapa:

1. **Estadía no ACTIVA** (pendiente/cerrada) ⇒ `409` `ESTADIA_NO_ACTIVA`.
2. **Producto o variante inexistente / no disponible** ⇒ `422`
   `ITEM_NO_DISPONIBLE` (con `errors[]` señalando el ítem).
3. **Producto con variantes sin variante elegida** ⇒ `422` `VARIANTE_REQUERIDA`.
4. **Reintento (misma `Idempotency-Key`, mismo payload)** ⇒ `201` con el pedido
   original (no duplica).
5. **Precio/total manipulado por el cliente** ⇒ ignorado; el servidor recalcula
   sobre variantes y promociones vigentes.
6. Pedido creado ⇒ se inicia el pago (`POST /pedidos/{id}/pago`), el resultado
   llega por webhook y se refleja por SSE (`pago.resultado`) y por
   `GET /pedidos/{id}/pago` (fallback polling).

---

## 3. Matriz endpoint → rol (insumo de la etapa 05)

Roles: **Pub** (sin auth) · **Cli** (cliente) · **Car** (carpero) · **Ope**
(operador) · **AdmB** (admin de balneario) · **SA** (super admin). ✔ = permitido.
El staff siempre restringido a **su** balneario (tenant del JWT).

| Endpoint | Pub | Cli | Car | Ope | AdmB | SA |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| `POST /auth/registro` · `login` · `refresh` | ✔ | | | | | |
| `POST /auth/logout` | | ✔ | ✔ | ✔ | ✔ | ✔ |
| `GET /balnearios`, `/{slug}`, `/branding`, `/menu`, `/promociones` | ✔ | | | | | |
| `POST /balnearios/{id}/estadias` (solicitar) | | ✔ | | | | |
| `GET /balnearios/{id}/estadias/actual`, `/estadias/mias` | | ✔ | | | | |
| `GET /estadias/{id}` (dueño) | | ✔ | | | | |
| `PATCH /estadias/{id}/ubicacion`, `POST /cierre` | | ✔ | | | | |
| `POST /estadias/{id}/pedidos`, `GET` lista | | ✔ | | | | |
| `GET /pedidos/{id}`, `POST /cancelacion` (dueño) | | ✔ | | | | |
| `GET /pedidos/{id}/pago`, `POST /pago` | | ✔ | | | | |
| `POST /webhooks/mercadopago` | ✔¹ | | | | | |
| `POST /estadias/{id}/solicitudes-servicio`, `GET` | | ✔ | | | | |
| `GET /operativo/pedidos`, `POST /transicion` | | | | ✔ | ✔ | |
| `GET /operativo/solicitudes-servicio`, `POST /transicion` | | | ✔ | | ✔ | |
| `GET /operativo/estadias/pendientes` | | | ✔ | | ✔ | |
| `POST /operativo/estadias/{id}/validacion` | | | ✔ | | ✔ | |
| `admin/**` (catálogo, ubicaciones, promos, staff, config visual, MP, reportes, dashboard) | | | | | ✔ | |
| `super-admin/**` (balnearios, planes, temporadas, suscripciones) | | | | | | ✔ |

¹ El webhook es público a nivel red pero **autenticado por firma** (`x-signature`),
no por JWT (etapa 05).

**Reglas finas** (a cerrar en etapa 05):
- Un cliente solo ve/opera **sus** estadías, pedidos y solicitudes (dueño =
  `cliente_id` del token). IDs ajenos ⇒ `404` (no se revela existencia).
- Un carpero valida estadías y atiende servicios **de su balneario**; el
  operador transiciona pedidos **de su balneario**. El admin de balneario
  hereda ambas capacidades operativas además de las de configuración.
- Toda ruta `admin/**` y `super-admin/**` rechaza cualquier otro rol con `403`.

---

## 4. Matriz pantalla (user journeys etapa 01) → endpoints

| Pantalla / paso | Endpoints |
|---|---|
| Selección de balneario | `GET /balnearios` |
| Transición white-label al elegir | `GET /balnearios/{id}/branding` |
| Solicitar apertura de estadía | `POST /balnearios/{id}/estadias` |
| Espera de validación del carpero | `GET /balnearios/{id}/estadias/actual` + SSE `estadia.validada` |
| Home de estadía (re-ingreso diario) | `GET /estadias/mias`, `GET /balnearios/{id}/estadias/actual` |
| Menú y producto (con variantes) | `GET /balnearios/{id}/menu` |
| Promociones | `GET /balnearios/{id}/promociones` |
| Carrito → confirmar → pagar | `POST /estadias/{id}/pedidos`, `POST /pedidos/{id}/pago` |
| Seguimiento de pedido en vivo | SSE `pago.resultado`/`pedido.estado`, `GET /pedidos/{id}` |
| Servicio al carpero | `POST /estadias/{id}/solicitudes-servicio` |
| Cierre de estadía con resumen | `POST /estadias/{id}/cierre` |
| Panel operativo — cola | `GET /operativo/pedidos` + SSE `pedido.nuevo` |
| Panel operativo — despachar | `POST /operativo/pedidos/{id}/transicion` |
| Panel operativo — validar estadía | `GET /operativo/estadias/pendientes`, `POST /.../validacion` |
| Panel operativo — servicios | `GET /operativo/solicitudes-servicio`, `POST /.../transicion` |
| Admin — menú/variantes/ubicaciones/promos/staff | `admin/categorias|productos|variantes|ubicaciones|promociones|usuarios` |
| Admin — theme con preview | `GET|PUT /admin/configuracion-visual` |
| Admin — vincular Mercado Pago | `GET /admin/mercadopago`, `POST /admin/mercadopago/oauth` |
| Admin — dashboard y reportes | `GET /admin/dashboard`, `GET /admin/reportes/{tipo}` |
| Super Admin — alta/gestión balnearios | `super-admin/balnearios`, `/planes`, `/temporadas`, `/suscripciones` |

Cobertura completa: cada paso de los user journeys de la etapa 01 se resuelve
con endpoints definidos.

---

## 5. Tiempo real (contrato del canal — ADR-003)

Dos canales SSE (`text/event-stream`), con **polling de respaldo** siempre
disponible (todo estado es reconstruible por GET):

| Canal | Endpoint | Eventos |
|---|---|---|
| Cliente | `GET /stream/cliente` | `estadia.validada`, `pago.resultado`, `pedido.estado`, `servicio.estado` |
| Operativo | `GET /stream/operativo` | `pedido.nuevo`, `servicio.nuevo`, `estadia.solicitud` |

- Reconexión automática con `Last-Event-ID`; heartbeat cada ~25 s.
- Payload de cada evento = el mismo DTO que devuelve el GET del recurso
  (ej. `pedido.estado` lleva un `Pedido`), para que mobile/web reusen el
  mismo parser.

---

## 6. Decisiones abiertas (no bloquean el contrato)

1. **Rotación/duración exacta de tokens** y revocación: se cierra en etapa 05
   (acá solo se fija que hay access+refresh Bearer).
2. **Firma del webhook de MP** (algoritmo y verificación de `x-signature`):
   detalle en etapa 05; el contrato ya expone el endpoint y su header.
3. **Formato exacto de cada reporte** (`filas[]` es genérico a propósito): las
   columnas se cierran en la etapa 15.
