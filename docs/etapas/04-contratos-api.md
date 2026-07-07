# Etapa 04 — Convenciones y contratos de API REST

- **Orden:** 04
- **Modelo ejecutor:** opus
- **Tipo:** especificación (documento, sin código)
- **Depende de:** 02, 03

## Objetivo

Definir el contrato completo de la API antes de escribir código, para que
backend, mobile y web puedan construirse en paralelo contra la misma
especificación.

## Alcance / Entregables

1. **Convenciones generales** (documento corto y normativo):
   - Versionado (`/api/v1`), nombres de recursos, verbos y códigos de estado.
   - Formato de error estándar (RFC 7807 Problem Details o equivalente propio).
   - Paginación, orden y filtros; formato de fechas (ISO 8601, TZ
     `America/Argentina/Buenos_Aires`); montos (decimal como string o minor
     units — decidir y justificar).
   - Cómo viaja el tenant: según ADR de etapa 02 (claim en JWT para staff,
     path/header para recursos públicos de un balneario).
   - Idempotencia en creación de pedidos (clave de idempotencia — la conexión en
     la playa es mala y habrá reintentos).
2. **Especificación OpenAPI 3** (`docs/api/openapi.yaml`) cubriendo todos los
   endpoints del MVP, agrupados por módulo:
   - **Público/cliente**: listado de balnearios, branding/configuración visual,
     menú y promociones vigentes, registro/login de cliente.
   - **Estadía**: solicitar apertura (queda `PENDIENTE_VALIDACION`), consultar
     activa/pendiente, cambiar ubicación, cerrar. La consulta de "mi estadía"
     es por balneario (un cliente puede tener estadías activas en varios
     balnearios a la vez).
   - **Pedidos**: crear desde carrito (con variantes elegidas por ítem), listar
     por estadía, detalle con historial de estados, cancelar (si el estado lo
     permite).
   - **Pagos (Mercado Pago, Checkout API)**: creación del pago contra la
     cuenta MP del balneario (`application_fee = 0`), consulta de estado de
     pago del pedido, y **endpoint receptor del webhook de Mercado Pago**
     (contrato del payload, validación de autenticidad según etapa 05,
     idempotencia de notificaciones repetidas).
   - **Servicios al carpero**: crear solicitud, listar, estado.
   - **Operativo (staff)**: cola de pedidos entrantes, transición de estados,
     cola de solicitudes de servicio, y **bandeja de validación de estadías**
     (el carpero confirma o rechaza aperturas pendientes).
   - **Admin balneario**: ABM menú/productos/categorías **con variantes**,
     ubicaciones, promociones, usuarios staff, configuración visual (theme
     white-label completo), estado/inicio de la vinculación OAuth con Mercado
     Pago, reportes.
   - **Super Admin**: ABM balnearios, planes, temporadas, activación/suspensión.
   - **Tiempo real**: contrato del canal elegido en etapa 02 (eventos, payloads,
     y su fallback por polling).
3. **Ejemplos de request/response** para los flujos críticos (abrir estadía,
   crear pedido, transición de estado) incluidos en la spec.
4. **Matriz endpoint → rol** (quién puede llamar qué), insumo directo de la
   etapa 05.

## Inputs requeridos

- Modelo de datos (etapa 03) y ADRs (etapa 02) aprobados.

## Criterios de aceptación

- La spec OpenAPI valida sin errores (lint con Spectral o similar).
- Cada pantalla de los user journeys de la etapa 01 puede resolverse con los
  endpoints definidos (matriz pantalla → endpoints incluida).
- Ningún endpoint tenant-scoped funciona sin tenant resuelto; los públicos
  exigen balneario explícito.
- El flujo "crear pedido" define comportamiento ante reintento (idempotencia)
  y ante producto sin stock/no disponible.
