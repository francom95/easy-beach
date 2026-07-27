# Etapa 16 — Mobile app cliente (React Native)

- **Estado:** ejecutada, con verificación en dispositivo parcial (ver §5).
- **Corresponde al plan:** [docs/etapas/16-mobile-app-cliente.md](../etapas/16-mobile-app-cliente.md)
- **Depende de:** 07 (diseño), 09–14 (API backend, ya completa desde el
  Hito Backend MVP de la etapa 15).
- **Código:** [`mobile/`](../../mobile/) — proyecto React Native 0.86 bare
  (sin Expo), TypeScript, `com.easybeach.mobile`.

## 1. Qué se construyó

La app del cliente completa contra la API real: base técnica (navegación,
cliente HTTP con refresh de token automático y single-flight, storage
seguro, estado global, manejo de errores), theming white-label dinámico en
runtime, y las ~33 pantallas de la etapa 07 (onboarding, selección de
balneario, apertura de estadía con espera de validación del carpero, home,
menú y detalle con variantes, carrito y checkout con pago in-app, tracking
en tiempo real, servicios al carpero, promociones, cierre de estadía,
estados no felices de zona 4).

Dos decisiones explícitas ya tomadas antes de escribir código, ambas
elegidas por el usuario vía pregunta directa:

- **Login por email + contraseña**, no el teléfono+SMS que proponía el
  mockup de la etapa 07 — el backend (etapa 09) y la etapa 05 ya usan
  email+contraseña; construir infraestructura SMS/OTP hubiera sido alcance
  no pedido.
- **React Native puro (bare)**, no Expo — implica manejar directamente los
  proyectos nativos `android/` e `ios/` (ver el hallazgo de build de §3).

## 2. Decisiones de arquitectura

- **Un solo stack de navegación plano** (~28 pantallas) más un
  `MainTabs` anidado (Inicio/Menú/Pedidos/Estadía) montado como screen del
  stack — no stacks anidados por zona. Simplifica navegar desde cualquier
  pantalla de detalle/checkout por encima de la tab bar.
- **Token de acceso solo en memoria** (nunca persistido: TTL de 60 min por
  etapa 05 §1.1), **refresh token opaco rotativo en Keychain**
  (`react-native-keychain`) — refresh single-flight en `api/client.ts` para
  que N requests en paralelo que reciben 401 no disparen N refresh en
  paralelo.
- **Theming**: cache-first en `AsyncStorage` (`theme:{slug}`) +
  revalidación en background (el endpoint de branding no tiene ETag, a
  diferencia del menú de etapa 11) + fallback al último theme cacheado si
  falla la red (nunca al neutro EasyBeach una vez cruzada la frontera
  white-label) + error solo si no hay cache en absoluto.
- **SSE como acelerador, nunca como reemplazo del polling**: cada pantalla
  relevante (S06, S09, S17, S20, S21, S23) ya hace polling propio vía
  `refetchInterval` de React Query — es el contrato de primera clase de
  ADR-003. `RealtimeProvider` (`react-native-sse`, la librería que
  ADR-003 marcaba "a validar en esta etapa") solo invalida cachés cuando
  llega un evento, acelerando el próximo render. **No se pudo mantener una
  sesión larga con SSE real conectado en este entorno** (ver §5) — el
  camino de polling sí se verificó real.
- **Pago**: sin credenciales de Mercado Pago disponibles en este entorno,
  siguiendo el mismo precedente que el propio backend (`FakeMercadoPago
  PaymentClient` de etapa 13). El checkout in-app (S16) genera un token de
  tarjeta puramente local y ficticio — documentado como stub explícito. Toda
  la lógica de negocio alrededor (idempotencia, revalidación previa al
  pago, reintento con clave nueva tras rechazo) es real y quedó cableada
  contra el backend real.
- **Ubicaciones**: se encontró un hueco real de integración — no existía
  ningún endpoint público para listar ubicaciones de un balneario (etapa 11
  solo tiene el menú público). Se agregó
  `GET /api/v1/balnearios/{slug}/ubicaciones` (`PublicUbicacionController`,
  `UbicacionService.listarActivas`, mismo patrón sin contexto de tenant que
  `MenuPublicoService`), con 3 tests de integración nuevos
  (`PublicUbicacionIntegrationTest`, 3/3 verde).

## 3. El hallazgo más importante: bug real de carrera encontrado en verificación con emulador

`S04SplashBalneario` decidía a qué pantalla navegar (S05 elegir ubicación,
S06 esperando validación, o directo a `MainTabs`) en un `useEffect` que
depende de `balnearioQuery`/`vigentesQuery`. Pero `entrarABalneario(slug,
nombre)` — la única función que escribe `balnearioSlug` en `stayStore`, y
de la que depende **toda** pantalla downstream (S05 lee `balnearioSlug` del
store, no de route params) — estaba encadenada *dentro* del `.then()` de
`cargarThemeDeBalneario(slug)`, una promesa completamente independiente y
con tiempos propios (fetch de branding + lectura/escritura de
`AsyncStorage`).

Como ambos efectos corren en paralelo sin sincronizarse entre sí, si el
theme tardaba más en resolver que el combo `balnearioQuery` +
`vigentesQuery` (plausible: dos GETs simples vs. fetch de branding +
AsyncStorage), `S05ElegirUbicacion` montaba con `balnearioSlug` todavía en
`null`, y `listarUbicaciones(null)` construía una URL rota — encontrado
exactamente así, en vivo, contra un balneario bootstrapeado de cero
(`balneario-test`) en el emulador: la pantalla mostraba "No pudimos cargar
las ubicaciones" pese a que `curl` directo al mismo endpoint devolvía 200
con datos.

**Fix**: `entrarABalneario(slug, nombre)` se llama de forma eager al montar
el efecto (slug/nombre ya son conocidos por los route params, no dependen
de ninguna promesa) — la carga del theme queda desacoplada, con su propio
manejo de error hacia S33SinConexion sin bloquear la entrada al balneario.
Verificado: recompilación limpia (`tsc --noEmit`, 0 errores) y re-ejecución
en el emulador, donde S05 ya cargó correctamente la ubicación real
bootstrapeada.

## 4. Otras decisiones de adaptación mockup↔backend (documentadas, no bugs)

- **S05**: el modelo real es plano (`tipo` + `identificador` de texto
  libre), no el número/chip estructurado del mockup — se agrupa por tipo y
  se listan los identificadores reales como filas.
- **S14** (producto agotado): el menú público (etapa 11) ya excluye
  productos no disponibles, así que esta pantalla es inalcanzable desde el
  browsing normal — se re-usa como destino del 422 `ITEM_NO_DISPONIBLE` de
  revalidación en checkout.
- **S26/S27** (promociones): el endpoint público no expone fechas de
  vigencia ni composición del combo — sin countdown inventado, y S27 queda
  solo informativo (el descuento se aplica solo del lado servidor).
- **S32** (cierre con pedidos en curso): la decisión de negocio de etapa 12
  es "bloquear, nunca forzar" — sin endpoint de override, S32 ofrece
  cancelar pedidos individualmente cancelables en vez de un botón ficticio.
- **S36** (`CERRADA_POR_SISTEMA`): estado real del modelo sin job que lo
  dispare todavía (deuda ya documentada en etapas 12/15) — pantalla lista
  pero hoy inalcanzable en la práctica.

## 5. Cómo se verificó

- **TypeScript**: `tsc --noEmit` limpio sobre las ~90 archivos de
  `mobile/src/` (1 error real encontrado y corregido:
  `StyleSheet.absoluteFillObject` no existe en esta versión de RN, es
  `absoluteFill`).
- **Backend**: suite completa (no solo el test nuevo) — **verde**, sin
  regresiones por el endpoint de ubicaciones agregado.
- **Build nativo Android real**: `gradlew app:installDebug` — encontró y
  resolvió un problema real de entorno, no de la app: la ruta del proyecto
  (`...\Montanari Technologies\Easy Beach\mobile\...`) excede el límite de
  260 caracteres de Windows una vez que CMake genera los nombres de objeto
  para el codegen C++ de `react-native-safe-area-context`/`rnscreens`. Se
  preguntó al usuario cómo resolverlo; eligió mapear el proyecto a una
  unidad corta (`subst R:`, reversible, sin tocar configuración del
  sistema) en vez de habilitar Windows Long Paths a nivel de registro.
  Build exitoso desde `R:\mobile\android`.
- **Verificación en dispositivo (Android emulator, `Medium_Phone_API_36.0`,
  con backend local real + MySQL en Docker + datos de tenant bootstrapeados
  a mano vía API — balneario, plan, temporada, suscripción, carpero,
  ubicación, producto)**: **confirmado real y funcionando** — registro de
  cuenta cliente contra el backend real (JWT emitido, navegación
  automática), login persistente, listado de balnearios operativos desde
  el backend (`balneario-test` recién creado), selección de balneario con
  transición hacia el theme (S03→S04), y el bug de §3 encontrado y
  corregido en esta misma sesión de verificación.
- **Lo que quedó sin verificar en dispositivo por inestabilidad del
  entorno, no por sospecha de bug de la app**: el emulador entró en ANR
  repetido y luego en un estado de `system_server` no responsivo bajo la
  carga combinada de MySQL+backend+Metro+emulador en esta máquina — se
  relanzó tres veces (incluyendo con más RAM y renderizado por software)
  sin lograr una sesión estable más allá de S05. El resto del flujo
  (validación del carpero, home, menú, carrito, checkout, SSE en vivo,
  cierre de estadía) está revisado por código y compila, pero **no tiene
  verificación visual en dispositivo** en esta sesión.

## 6. Deuda explícita

- **Verificación visual en dispositivo incompleta** (ver §5) — pendiente
  repetir en un entorno con más margen de recursos, o en un dispositivo
  físico.
- **SSE sin verificación de conexión real sostenida** — el camino de
  polling (contrato de primera clase) sí se verificó.
- **Pago Mercado Pago es un stub local** — sin credenciales reales de MP en
  este entorno, mismo precedente que el backend. La lógica de idempotencia/
  reintento alrededor es real.
- **Notificaciones push: fuera de MVP**, sin ADR de etapa 02 que las
  incluya — solo in-app/polling-SSE, según la instrucción condicional del
  propio plan de esta etapa.
- **iOS**: no se intentó build/verificación (sin macOS disponible en este
  entorno) — el código no tiene nada Android-específico fuera de
  `api/config.ts` (el `10.0.2.2` vs `localhost`), pero el proyecto nativo
  `ios/` generado por el CLI nunca se compiló.
- Deuda heredada de etapas anteriores (job de reconciliación de pagos, SSE
  de instancia única, job de cierre administrativo de estadías) sigue
  igual — no la resuelve ni la agrava esta etapa.
