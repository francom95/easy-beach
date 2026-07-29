# Revisión final de cierre (post-etapa 20)

Auditoría de las 20 etapas contra el código realmente construido, hecha después
de dar el plan por terminado. El criterio fue el opuesto al de los entregables:
no confirmar que todo está bien, sino **encontrar dónde lo entregado no cumple
lo prometido**.

Resultado corto: la calidad de lo construido es alta (159/159 tests, cero
TODOs/FIXMEs en `backend/src/main`, `mobile/src` y `web/src`, sin secretos
commiteados), pero aparecieron dos patrones de problema: **piezas de resiliencia
de pagos que quedaron a medio cablear** y **documentación de especificación que
dejó de seguir al código**.

## 1. Arreglado en esta revisión

### 1.1 La suite de tests de mobile nunca funcionó

`mobile/jest.config.js` apuntaba a `@react-native/jest-preset`, que no estaba en
`devDependencies`. Como tampoco había workflow de CI para mobile (sí para
backend y web), nada lo detectaba. Se agregó el preset, los
`transformIgnorePatterns` de los paquetes ESM que la app importa, `jest.setup.js`
con los mocks de módulos nativos, y `.github/workflows/mobile-ci.yml`
(lint + tsc + test). De paso se limpiaron 6 variables/imports sin usar que
hacían fallar el lint.

### 1.2 La cola de pedidos del panel operativo mostraba "undefined"

`web/src/api/types.ts` declaraba `productoNombre`/`varianteNombre`; el backend
serializa `nombreProducto`/`nombreVariante` (`PedidoResponse.java`, sin
`NamingStrategy`). Cada ítem de cada tarjeta de la pantalla central del panel
renderizaba `2× undefined`. Mobile los tenía bien: era exclusivo de la etapa 17,
cuyo entregable declara esa pantalla "verificada en navegador real".

El mismo drift afectaba a `promociones` (declarado `string[]`, el backend manda
`{nombre, montoDescuento}[]`), a `PedidoEventoResponse` (sus 4 campos mal) y
faltaba `subtotalLinea`. Se corrigió el bloque completo contra los DTOs reales.

**Verificado en navegador**: con el perfil demo, la tarjeta ahora muestra
`2× Cerveza`, sin errores de consola.

### 1.3 Vincular Mercado Pago abría un popup en blanco

`IniciarVinculacionResponse` en web declaraba `url`; el backend devuelve
`urlAutorizacion`. `window.open(undefined)` abre `about:blank`. No dependía de
credenciales reales de MP: estaba roto del lado cliente. Verificado contra la
respuesta real del endpoint.

### 1.4 No existía el job de reconciliación de pagos (ADR-004)

La query (`PedidoPagoRepository.findByEstadoAndCreatedAtBefore`) estaba escrita y
comentada como "job de reconciliación", pero **no tenía ni un solo llamador**.
Consecuencia real: si MP no entregaba el webhook, el cliente pagaba, el pedido
nunca entraba a la cola de la cocina, y nadie se enteraba.

Se agregó `PagoReconciliacionJob` (cada 5 min, sobre pagos PENDIENTE de más de
10 min). La lógica de "aplicar el estado real que reporta MP" se extrajo a un
método compartido con el webhook: son dos puertas de entrada al mismo hecho y
las invariantes de plata no pueden divergir (no retroceder un pago resuelto, no
confirmar si el monto no coincide, publicar `PagoResuelto` al resolverse).

Cada pago se concilia en su propia transacción: los tokens son por balneario,
así que una cuenta de MP caída no puede dejar sin conciliar a los demás. Hay un
test que cubre exactamente eso.

### 1.5 No existía el refresh de tokens OAuth de MP (ADR-004)

`MercadoPagoOAuthClient.refreshToken()` estaba implementado y nunca se invocaba;
`tokenExpiraAt` se escribía y no se leía en ningún lado. Al vencer el token, el
balneario dejaba de poder cobrar **en silencio**.

Se agregó `MpTokenRefreshJob` (cada 6 h, renueva con 7 días de anticipación).
Decisión de diseño: si el refresh falla pero el token sigue vigente se asume
transitorio y se reintenta; si falla y el token **ya venció**, la credencial pasa
a `EXPIRADA`, que corta el paso a pedidos nuevos en vez de dejarlos fallar recién
en el cobro.

Dos cosas que salieron de escribir esto:
- El desenlace se devuelve como valor y no como excepción: marcar `EXPIRADA` y
  después relanzar haría rollback de ese mismo cambio (misma trampa
  transaccional que la etapa 19 documentó).
- El fake de OAuth devolvía un TTL de 6 h, más corto que el margen del job — con
  eso, cada token recién renovado volvería a entrar en la ventana de "por vencer"
  y el job lo refrescaría en cada ciclo. Se lo alineó al TTL real de MP
  (180 días) y se dejó comentado por qué importa.

## 2. Hallazgos NO arreglados

Quedan documentados acá para que no se pierdan. Ninguno se tocó en esta pasada.

### Alta

| # | Hallazgo | Dónde |
|---|---|---|
| A1 | **Rate limiting: cero implementación.** `05-seguridad-roles.md` §6 lo especifica en detalle (login 5/min por IP+email → 429, creación de pedido, registro, webhook) y la mitigación de la amenaza STRIDE #7 está asignada a la etapa 09, marcada ✅. No hay ningún límite en el backend ni en el `Caddyfile`. El login está expuesto a fuerza bruta sin freno. | etapas 05/09 |
| A2 | **`docs/api/openapi.yaml` es ficción.** Último cambio en la etapa 05; las etapas 09-20 nunca lo tocaron. 33 operaciones documentadas no existen tal como están escritas y hay ~50 implementadas fuera del contrato (`/pedidos/{id}/pago` no existe, 7 endpoints documentados `PATCH` son `PUT` y no hay un solo `@PatchMapping`, el webhook está en otra ruta, los públicos usan `{slug}` y no `{balnearioId}`). Relevante porque `mobile/src/api/types.ts` lo cita como fuente de verdad. | etapa 04 |
| A3 | **La app mobile no tiene forma de apuntar a producción.** `mobile/src/api/config.ts` fija `10.2.2.2:8080` sin variable de entorno ni build arg, y `docs/deploy/mobile-build-y-publicacion.md` nunca lo menciona. Un APK firmado siguiendo ese documento apunta al loopback del emulador. | etapa 20 |
| A4 | **Los códigos de error de negocio del contrato no existen.** `04-contratos-api.md` nombra seis códigos estables (`ESTADIA_DUPLICADA`, `TRANSICION_INVALIDA`, `ITEM_NO_DISPONIBLE`, …); `ErrorCode.java` no define ninguno: todo cae en `CONFLICTO_DE_ESTADO` o `VALIDACION_FALLIDA`. Además el array `errors[]` usa `field`/`message` contra `campo`/`mensaje` del schema. | etapa 04 |

### Media

- **ADR-003**: tres de los siete eventos SSE declarados nunca se emiten (el módulo `stay` no emite ninguno), y no hay `Last-Event-ID` ni buffer de reposición pese a que el ADR los exige. Mobile registra handlers para dos eventos que el backend nunca manda.
- **ADR-002 regla 2**: 7 módulos acceden a repositorios/entidades JPA de otros módulos, y el test de ArchUnit **no verifica esa regla** — el ADR afirma que está "verificada en CI" y no lo está. Regla 1 (capas): 8 controllers/mappers inyectan repositorios directo.
- **Producto agotado cableado al código equivocado**: el backend tira 409 y `S15Carrito.tsx` navega a `S14ProductoAgotado` solo con 422. La pantalla es inalcanzable en su caso de uso, y el 422 real muestra el mensaje equivocado.
- **S35 (revalidación del carrito) comentado pero no implementado**: la regla dura de la etapa 07 ("nunca se cobra un precio distinto al mostrado") no está.
- **El entorno demo no puede crear pedidos**: el seeder siembra una credencial MP falsa y el cliente real hace HTTP a MP, así que `POST /pedidos` falla bajo `local,demo` — justo la acción estrella para mostrarle a un balneario.
- **No existe la alerta sonora/visual de pedido nuevo** (entregable 2 del alcance de la etapa 17), y no figura como deuda en el entregable.
- **El panel descarta los mensajes accionables del backend**: el 409 de concurrencia de la etapa 19 se muestra como un toast fijo genérico; `ProblemDetail` de web omite `errors[]`.
- **Índices**: `03-modelo-de-datos.md` §5 afirma que todo índice tenant-scoped arranca por `balneario_id`; 13 no lo hacen. `mp_webhook_notificacion.balneario_id` es NULL, rompiendo la invariante de ADR-001.
- **Baja de cuenta con anonimización de PII**: prometida en `05-seguridad-roles.md` §5, no implementada. Sin redacción automática de logs.
- **Auditoría de Super Admin incompleta**: `PlanService` y `TemporadaService` no auditan (cambiar el estado de una temporada no deja rastro).

### Criterios ✅ que no se cumplieron

Los entregables son honestos en sus secciones de deuda, pero los archivos
`docs/etapas/NN-*.md` marcan "✅ EJECUTADA" sin calificar:

- **Etapa 19**: los E2E no corrieron en staging (no existía hasta la etapa 20),
  ni con la app mobile, ni contra el sandbox de MP.
- **Etapa 20**: la app no está en review en ninguna store, no hay dominio con
  TLS, y `deploy.yml` nunca se ejecutó (no hay VPS).
- **Etapa 16**: iOS nunca se compiló.

## 3. Estado

- Backend: **159/159 tests, BUILD SUCCESS** (151 previos + 8 nuevos de los dos jobs).
- Web: lint + `tsc --noEmit` sin errores.
- Mobile: lint + `tsc --noEmit` + 1 test, sin errores (antes la suite no corría).
