# Etapa 17 — Web: panel operativo y panel admin de balneario

- **Orden:** 17
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 08 (diseño), 10–15 (API)
- **Estado: ✅ EJECUTADA**, verificada en navegador real — código en
  [`web/`](../../web/) (Next.js 16, TypeScript), entregable en
  [`docs/especificacion/17-web-panel-operativo-admin.md`](../especificacion/17-web-panel-operativo-admin.md).
  `tsc`/`eslint` limpios, suite backend 113/113. Hallazgo crítico: CORS
  bloqueaba el 100% del panel (preflight `OPTIONS` rechazado con 401 por
  Spring Security antes de que `CorsFilter` respondiera) — invisible hasta
  ahora porque el `fetch` de React Native (mobile) nunca hace preflight;
  corregido con un `permitAll()` explícito para el preflight. También
  encontró y corrigió una violación real de ADR-002 (un intento de mover
  `/staff/balneario` a `identity.web` fue rechazado en el momento por el
  propio `ModuleDependencyRulesTest`).

## Objetivo

Construir la aplicación web (React/Next.js) para el personal del balneario:
el panel operativo (despacho de pedidos y servicios en temporada) y el panel de
administración (configuración y reportes).

## Alcance / Entregables

1. **Base técnica**: proyecto Next.js, autenticación de staff (etapa 09),
   control de acceso por rol en rutas, cliente API tipado (generado desde la
   spec OpenAPI de la etapa 04 si es viable), manejo de errores global.
2. **Panel operativo** (diseño de la etapa 08):
   - Cola de pedidos en vivo (canal de tiempo real + fallback polling),
     transición de estados en una interacción, alerta sonora/visual de pedido
     nuevo, indicador de antigüedad/demora.
   - Cola de solicitudes de servicio al carpero con su ciclo.
   - **Bandeja de validación de estadías**: el carpero confirma o rechaza las
     solicitudes de apertura pendientes (etapa 12); al confirmar, el cliente
     queda habilitado para pedir.
   - Optimizado para tablet; usable con targets grandes.
3. **Panel admin de balneario**:
   - Dashboard con el endpoint de KPIs (etapa 15).
   - ABM de menú con toggle rápido de disponibilidad, categorías con orden,
     **variantes por producto**, subida de fotos; ubicaciones; promociones con
     creación guiada por tipo; usuarios staff.
   - Configuración visual del **theme white-label completo** con preview en
     vivo (renderizar con los mismos tokens que usa mobile).
   - Vinculación de la cuenta de Mercado Pago del balneario (OAuth) con estado
     visible y advertencia si falta (sin ella no se cobran pedidos).
   - Reportes con filtros de fecha y export CSV (etapa 15).
4. **Estados vacíos y onboarding** del balneario nuevo (flujo "primer menú
   publicado" de la etapa 08).

## Inputs requeridos

- Mockups de la etapa 08.
- API de las etapas 10–15 en entorno accesible.

## Criterios de aceptación

- Un pedido creado desde la app mobile aparece en el panel operativo sin
  recargar, y su transición de estado llega al cliente.
- Todo el ciclo de administración (crear categoría → producto → foto →
  disponibilidad → promoción) funciona de punta a punta.
- El preview de configuración visual coincide con lo que muestra la app mobile.
- Un usuario operador no accede a las rutas de admin (y viceversa según matriz
  de la etapa 05).
