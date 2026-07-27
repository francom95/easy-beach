# Etapa 17 — Web: panel operativo y panel admin de balneario

- **Estado:** ejecutada y verificada en navegador real.
- **Corresponde al plan:** [docs/etapas/17-web-panel-operativo-admin.md](../etapas/17-web-panel-operativo-admin.md)
- **Depende de:** 08 (diseño), 10–15 (API backend).
- **Código:** [`web/`](../../web/) — Next.js 16 (App Router), TypeScript.

## 1. Qué se construyó

Los dos paneles de staff en alcance (Super Admin queda para la etapa 18):
login único por rol, panel operativo (cola de pedidos con 3 columnas y
transición de estado en un tap, cola de servicios al carpero, bandeja de
validación de estadías, header con indicador en vivo), y panel admin de
balneario completo — dashboard, ABM de menú (categorías, productos,
variantes, foto, disponibilidad instantánea), ubicaciones (incl. alta
masiva), staff (invitar/revocar), tipos de servicio, promociones (los 3
tipos con wizard guiado), identidad visual (editor de theme con validación
de contraste + preview en vivo), cobros (vinculación OAuth de Mercado
Pago), reportes (5 reportes con filtro de fecha + export CSV), y un
checklist de onboarding para balnearios nuevos.

## 2. Decisiones de arquitectura

- **Next.js 16** (recién liberado — bare mínimo de mi conocimiento previo de
  Next.js): el propio scaffold trae un `AGENTS.md` avisando "esta versión
  tiene cambios rotos, leé los docs antes de escribir código" — se leyó
  `node_modules/next/dist/docs/01-app/02-guides/upgrading/version-16.md`
  antes de tocar nada. Cambios relevantes: `middleware.ts` → `proxy.ts`
  (no usado acá, ver más abajo), `params`/`searchParams` ahora son
  Promises, Turbopack por defecto, `next lint` removido (se corre
  `eslint .` directo).
- **Todo Client Components**: dado que este panel es esencialmente una SPA
  sobre una API REST ya completa (sin necesidad de SSR/RSC), casi todas las
  pantallas son `'use client'` con React Query haciendo fetch directo al
  backend desde el navegador — evita la complejidad de caching/Server
  Actions de Next 16 casi por completo. Mismo patrón de auth que mobile
  (etapa 16): access token en memoria, refresh token persistido (acá en
  `localStorage`, sin Keychain en un navegador — aceptable para un panel
  interno de staff servido por HTTPS en producción, documentado como
  adaptación).
- **Sin `middleware`/`proxy` para guardas de ruta**: la sesión vive en
  memoria + localStorage, no en cookies — un middleware server-side no
  puede leerla. El guard (`RequireAuth`) es 100% client-side.
- **SSE con `@microsoft/fetch-event-source`, no `EventSource` nativo**: el
  canal `/stream/operativo` exige el header `Authorization`, y el
  `EventSource` del DOM no puede mandar headers custom (limitación conocida
  del navegador) — mismo problema exacto que ya había resuelto
  `react-native-sse` del lado mobile (etapa 16), acá con la librería
  equivalente para navegador. Sigue siendo un acelerador sobre el polling
  (ADR-003), nunca un reemplazo — no cubre la bandeja de estadías (el canal
  no emite ese evento, gap ya documentado en etapa 16).
- **Marca EasyBeach fija para los paneles de staff** (etapa 08: "los tres
  llevan marca EasyBeach"): tokens propios en `globals.css` (mismos valores
  hex que el theme neutro de `docs/design/tokens.md` / mobile), tipografía
  Bricolage Grotesque (display) + Lexend (body) + JetBrains Mono
  (tags/código) vía `next/font/google` — nunca el theme white-label del
  balneario, que solo se renderiza en el preview de Identidad visual.

## 3. El hallazgo más importante: CORS bloqueaba TODA la app, invisible hasta un navegador real

Con el backend y el panel corriendo, el primer login funcionó pero **cada
llamada subsiguiente devolvía `net::ERR_FAILED`** — el dashboard, el menú,
las colas operativas, todo. La causa: cualquier request con header
`Authorization` dispara un preflight `OPTIONS` del navegador, y
`SecurityConfig` tenía `anyRequest().authenticated()` sin ninguna excepción
para el preflight — Spring Security exigía autenticación al `OPTIONS`
mismo (que nunca trae el header, es la negociación *previa* a mandarlo),
devolviendo 401 antes de que el `CorsFilter` ya configurado llegara a
responder.

**Por qué nunca apareció hasta ahora**: el `fetch` de React Native (mobile,
etapa 16) no implementa same-origin policy ni hace preflight — nunca
ejercitó este camino. Recién un navegador real, que sí lo hace siempre que
hay un header custom, lo expuso. Un gap de infraestructura de 4 etapas
atrás, invisible en cualquier test de integración (`TestRestTemplate`
tampoco preflight-ea) y en la app mobile, y que **habría bloqueado el 100%
del panel web en producción**.

**Fix**: `SecurityConfig` ahora permite explícitamente el preflight
(`.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()`) antes de
`anyRequest().authenticated()`. Verificado con un preflight real
(`curl -X OPTIONS` con `Origin`/`Access-Control-Request-*`) devolviendo 200
con los headers CORS correctos, y confirmado en el navegador: las mismas
llamadas que fallaban pasaron a 200 sin ningún otro cambio.

## 4. Otros gaps de backend reales encontrados y resueltos

Mismo patrón que etapa 16 (ubicaciones): construir el consumidor real
expuso huecos que ninguna especificación había anticipado con precisión.

- **`GET /api/v1/admin/balneario`**: ni el token de staff ni
  `/staff/whoami` exponían nombre/slug/estado del propio balneario, que
  todo el sidebar/header del admin necesita. Se agregó un controller
  delgado en `platform.web` (no `identity.web`: ver nota de arquitectura
  abajo).
- **`GET /api/v1/staff/balneario`**: lo mismo pero para CARPERO/OPERADOR
  (el header del panel operativo también lo necesita, no solo el admin).
  **Primer intento roto**: agregarlo directo a `StaffController`
  (`identity.web`) — el propio `ModuleDependencyRulesTest` (ArchUnit) lo
  rechazó en el momento: `identity` no puede depender de `platform`
  (ADR-002). Se movió a un controller nuevo en `platform.web`
  (`StaffBalnearioController`), que sí puede depender de su propio
  `BalnearioService`. Un ejemplo real de la gobernanza automática de
  arquitectura del proyecto atajando una violación en el momento, no
  después.
- **ABM real de staff** (`POST/GET/DELETE /api/v1/admin/staff`):
  `/staff/miembros` (etapa 09) era explícitamente de solo lectura
  ("los ABMs reales llegan en etapas 10-15" - nunca llegaron). Se agregó
  invitar (mismo patrón sin email real que el alta de admin de balneario:
  password temporal en la respuesta) y revocar (borra el vínculo
  `usuario_balneario_rol`, nunca el `Usuario` - preserva su historial en
  auditoría/`pedido_evento`). Guardas: solo CARPERO/OPERADOR invitables,
  email duplicado rechazado, no se puede revocar al único
  ADMIN_BALNEARIO.
- **`clienteNombre` en la bandeja de validación**: `EstadiaResponse` nunca
  expuso el nombre del cliente (el cliente no necesita ver su propio
  nombre reflejado). El mockup de etapa 08 lo pide explícito ("Marcos
  Iribarne · MI"). Se creó `EstadiaPendienteResponse` — un DTO nuevo, no un
  cambio al contrato que ya consume mobile — solo para
  `GET /operativo/estadias/pendientes`.
- **Foto de producto**: `ProductoRequest` no tenía forma de subir una
  imagen, a diferencia de los assets de branding. Se generalizó
  `AssetStorageService` (movido de `branding.storage` a `shared.storage`:
  `catalog` no puede depender de `branding`, ADR-002 — mismo tipo de
  restricción que el punto anterior) y se agregó
  `POST /admin/productos/{id}/foto`.

## 5. Adaptaciones documentadas (mockup vs. backend real)

- **Alta masiva de ubicaciones**: el mockup pide "1→64" como acción de
  primera clase; no hay endpoint bulk. Se resuelve 100% en cliente (un
  `POST /admin/ubicaciones` por unidad, en loop) — funcionalmente
  equivalente, sin necesidad de tocar el backend.
- **"Menú publicado / N cambios sin publicar"**: no existe un estado de
  borrador — cada cambio (categoría, producto, disponibilidad) es
  instantáneo, decisión ya tomada en etapa 11. El botón "Guardar y publicar
  theme" de Identidad visual sí aplica de verdad (ahí "publicar" es real:
  el PUT persiste y el público lo ve al toque); para Menú no se inventó un
  estado de borrador ficticio.
- **Mercado Pago, tarjeta "Vinculada"**: el mockup pide cuenta/fecha de
  vinculación/tipo de renovación/comisión; `EstadoVinculacionResponse` solo
  trae `{vinculado, mpUserId, estado}`. Se construyó la UI con lo que el
  contrato realmente expone, sin fabricar campos.
- **Callback de OAuth de MP**: el propio backend (etapa 10) lo documenta
  como placeholder ("responde JSON, etapa 17/18 lo convierte en un
  redirect real"). Se resolvió sin tocar el backend: el admin vincula en
  una ventana popup (`window.open`), y al volver el foco a la pestaña
  principal (`window.addEventListener('focus', ...)`) se refresca
  `/admin/mercadopago/estado` — no se probó contra Mercado Pago real (sin
  credenciales en este entorno, mismo límite que etapa 16).
- **Reportes de ventas sin desglose de descuentos/reembolsos**: el mockup
  pide tiles de Bruto/Descuentos/Reembolsos/Neto; `VentasReporteResponse`
  solo tiene el total ya neto de descuentos (`descuentoTotal` existe en
  `Pedido` pero no está agregado en el reporte). Se construyó con los
  campos reales (facturación, cantidad, ticket promedio, desglose por
  día) en vez de inventar una fila que el backend no puede sostener.

## 6. Cómo se verificó

- **TypeScript**: `tsc --noEmit` limpio en todo `web/src/`.
- **ESLint**: `eslint .` limpio — encontró 2 problemas reales del lint
  `react-hooks/set-state-in-effect` (setState síncrono dentro de un
  efecto, patrón de cascading render) en `AuthProvider` e
  `IdentidadVisualPage`; ambos refactorizados (el segundo, a estado
  derivado calculado en render en vez de vía efecto — el patrón
  recomendado por React, no solo un silenciado del lint).
- **Backend**: suite completa — **113/113, BUILD SUCCESS** — incluye los 4
  tests nuevos de esta etapa (`AdminBalnearioIntegrationTest` +2 casos,
  `AdminStaffIntegrationTest`, `ProductoFotoIntegrationTest`, más la
  corrección de `EstadiaCicloDeVidaIntegrationTest` para el nuevo DTO de
  pendientes) y el `ModuleDependencyRulesTest` (ArchUnit) verde tras la
  corrección de arquitectura.
- **Navegador real** (backend + MySQL locales, balneario y staff de la
  etapa 16 reutilizados, datos frescos creados vía API para esta sesión):
  confirmado extremo a extremo — login de staff con persistencia de sesión
  entre navegaciones; checklist de onboarding reflejando el estado real
  (ubicación/categoría/producto hechos, MP pendiente); ABM de menú con
  datos reales; Ubicaciones con datos reales; invitar/listar staff (creó
  un OPERADOR real con password temporal); guardado de Identidad visual
  persistido y confirmado idéntico en el endpoint público que consume
  mobile; creación de una promoción DESCUENTO_PORCENTUAL con alcance real;
  **cola de pedidos**: un pedido insertado en estado CONFIRMADO apareció en
  la columna "Nuevos", "Tomar" lo movió a "En preparación" con el botón
  cambiando a "Despachar"; **bandeja de estadías**: una solicitud pendiente
  mostró el nombre real del cliente ("Maria Iribarne", iniciales "MI") y
  "Confirmar" la validó correctamente; **cola de servicios**: una solicitud
  con nota apareció y se pudo tomar; Reportes con datos reales por pestaña
  (ej. 2 aperturas de estadía del día).
- **Pago real de Mercado Pago no verificado** (mismo límite que etapa 16:
  sin credenciales de MP en este entorno) — se insertó un pedido
  directamente en `CONFIRMADO` vía SQL para probar la cola/transiciones/
  SSE sin depender del flujo de cobro.

## 7. Deuda explícita

- **Vinculación de Mercado Pago no probada contra la API real** — solo el
  flujo de apertura de popup + refresh al volver el foco, sin credenciales
  reales para completar el OAuth.
- **SSE sin verificación de reconexión sostenida** — el polling (contrato
  de primera clase) sí se verificó real end-to-end.
- **Reportes de ventas sin desglose bruto/descuentos/reembolsos** (ver
  §5) — requeriría extender `VentasReporteResponse` en el módulo
  `reporting`, no se hizo en esta etapa.
- **Sin flujo de "cambiar contraseña obligatorio"** en el panel: el login
  de staff con password temporal responde `debeCambiarPassword: true`, y
  el panel no tiene todavía una pantalla dedicada para forzarlo (queda
  logueado igual, sin bloquear). Gap real, no crítico para esta
  verificación.
- Deuda heredada de etapas anteriores (job de reconciliación de pagos, SSE
  de instancia única, job de cierre administrativo de estadías por fin de
  temporada) sigue igual.
