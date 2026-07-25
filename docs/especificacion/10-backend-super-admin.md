# Etapa 10 — Backend Super Admin: balnearios, planes y temporadas

- **Estado:** ejecutada. Insumo directo de las etapas 11-15 (todas necesitan
  un balneario operativo, branding y credencial MP ya resueltos por esta etapa).
- **Corresponde al plan:** [docs/etapas/10-backend-super-admin.md](../etapas/10-backend-super-admin.md)
- **Depende de:** [09 (backend fundacional)](09-backend-fundacional.md).
- **Código:** [`backend/`](../../backend/) — módulos `platform`, `branding`, `payments` (OAuth).

## 1. Qué se construyó

### ABM de balnearios (`platform`)
`POST/PUT/GET /api/v1/super-admin/balnearios` — alta crea el balneario **y**
su primer usuario `ADMIN_BALNEARIO` con password temporal (12 caracteres,
alfabeto sin ambigüedad `0/O`, `1/l`) devuelta **una sola vez** en la
respuesta (sin servicio de email todavía, fuera de alcance). El usuario
queda con `debe_cambiar_password=true` (nuevo flag, etapa 05 §1.1) —
agregué `PUT /api/v1/auth/cambiar-password` (identity) para cerrar ese
círculo: valida el password actual, revoca **todas** las sesiones activas
del usuario (etapa 05 §1.3), tal como exige la spec.

`esOperativo(balnearioId)` = `estado=ACTIVO` **y** existe una
`suscripcion_temporada` `ACTIVA` en una `temporada` `EN_CURSO`. Es el hook
que etapas futuras (12: estadía, 13: pedidos) van a consultar para "un
balneario suspendido o fuera de temporada no acepta estadías ni pedidos
nuevos". Suspender/activar balneario y suspender/reactivar solo la
suscripción son dos palancas independientes, ambas afectan `esOperativo`.

### Planes, temporadas y suscripciones (`platform`)
ABM de `Plan` y `Temporada` (máquina de estados `PLANIFICADA → EN_CURSO →
CERRADA`, transiciones validadas). `SuscripcionTemporadaService.suscribir()`
crea la suscripción directamente en `ACTIVA` (decisión de producto: suscribir
= habilitar, no hay paso intermedio de aprobación en el MVP); transiciones
`PENDIENTE→ACTIVA`, `ACTIVA↔SUSPENDIDA`, `(ACTIVA|SUSPENDIDA)→FINALIZADA`.
Todo cross-tenant intencional (Super Admin), auditado.

### Vinculación OAuth con Mercado Pago (`payments`, adelantado de la etapa 13)
Flujo completo: `GET /api/v1/admin/mercadopago/oauth/iniciar` genera un
`state` anti-CSRF (persistido con TTL de 10 min) y devuelve la URL de
autorización de MP; `GET /api/v1/mercadopago/oauth/callback` (**público** —
MP redirige el navegador sin JWT) resuelve el balneario por `state`,
intercambia el código por tokens y los guarda **cifrados AES-256-GCM**
(`TokenEncryptionService`, clave efímera en local/dev, obligatoria en prod).
`BalnearioMpCredencialService.puedeRecibirPagos(balnearioId)` es el hook que
la etapa 13 va a exigir antes de crear un pago.

La implementación real de `MercadoPagoOAuthClient` (`RestClient` contra la
API de MP) está `@Profile("!test")`; en tests se reemplaza por un fake
determinístico (`FakeMercadoPagoOAuthClient`) — es el único límite externo
mockeado en toda la suite (no hay credenciales reales de una app de MP para
golpear en CI); todo lo demás corre contra MySQL real.

### Theming white-label completo (`branding`)
`ThemeTokenAssembler` arma el JSON de 25 tokens exacto del contrato de la
etapa 06 (`docs/design/tokens.md`): valida contraste WCAG de cada color
personalizable contra el mejor candidato de `on-*` (blanco vs. tinta
oscura), y si no cumple ≥4.5:1, propone el tono más cercano que sí cumple
(`ColorMath.nearestCompliantTone`, ajuste de luminosidad en HSL) — el
guardado exige aceptar la sugerencia (`aceptarSugerencia=true`) o corregir a
mano, nunca guarda un theme inválido. Los `on-*`/`muted`/`border` **siempre**
se derivan en el servidor, jamás llegan en el request.

> **Simplificación documentada:** la derivación de `muted`/`border` usa un
> blend lineal RGB/HSL, no la conversión OKLCH exacta que describe
> `tokens.md`. Implementar OKLCH correctamente (sRGB→lineal→OKLab→OKLCH)
> sin poder verificarlo visualmente tenía riesgo real de un error sutil de
> color; HSL logra el mismo objetivo cualitativo (tono más claro/oscuro,
> mismo matiz) con matemática mucho más simple de auditar. Revisar en un
> pase de diseño si el resultado no convence visualmente.

Assets (`POST /api/v1/admin/branding/assets/{tipo}`): valida **magic bytes**
reales (PNG/JPEG/SVG sin `<script`), no la extensión declarada; tamaño
máximo 5 MB; renombra a UUID; guarda fuera de `src/main/resources` (fuera
del classpath/web root); sirve por `GET /public/assets/...` que solo lee
bytes de disco, nunca interpreta el contenido.

`BalnearioCreadoListener` (evento de dominio, no llamada directa — ver §2)
siembra el theme default de EasyBeach al crear un balneario.

## 2. Bugs reales encontrados y corregidos durante la verificación

1. **Violación de ADR-002 al sembrar el branding default.** `BalnearioService`
   (`platform`) necesitaba invocar `ConfiguracionVisualService`
   (`branding`) al crear un balneario, pero la dependencia declarada va al
   revés (`branding → platform`). **Fix:** `BalnearioService` publica un
   evento `BalnearioCreado` (`ApplicationEventPublisher`); un
   `@EventListener` en `branding` lo escucha y siembra el default — corre
   **síncrono, en la misma transacción**, así que si sembrar fallara, la
   creación del balneario se revierte también.
2. **`EasyBeachUserPrincipal`/`RolCodigo`/`TipoUsuario` bloqueaban a
   `branding`/`payments`.** Sus controllers necesitan
   `@AuthenticationPrincipal EasyBeachUserPrincipal` para leer el tenant,
   pero esa clase (y los enums que usa) vivían en `identity`, que
   `branding`/`payments` no pueden depender de. **Fix:** las tres clases se
   movieron a `shared.security` — es vocabulario de autorización
   transversal, no lógica de negocio de `identity`.
3. **Hibernate rechaza `@FilterDef` duplicado.** Con una sola entidad
   `@TenantScoped` (etapa 09) no había problema; al agregar 5 más, cada una
   con su propio `@FilterDef(name="tenantFilter", ...)` idéntico, Hibernate
   tiró `AnnotationException: Multiple '@FilterDef' annotations define a
   filter named 'tenantFilter'`. **Fix:** el `@FilterDef` se declara **una
   sola vez**, a nivel de paquete (`shared.tenancy.package-info.java`); cada
   entidad `@TenantScoped` solo lleva `@Filter` (referencia, no redefinición).
4. **`Argon2PasswordEncoder` sin su implementación real.** Al corregir la
   etapa 09 (que usaba BCrypt por default) para cumplir "argon2id" de la
   etapa 05, el encoder de Spring Security delega en Bouncy Castle, que
   **no** es una dependencia transitiva — fallaba en runtime con
   `NoClassDefFoundError: org/bouncycastle/crypto/params/Argon2Parameters`.
   **Fix:** agregada `org.bouncycastle:bcprov-jdk18on` (runtime).
5. **Columna `password_hash` demasiado angosta para argon2id.** El hash
   encodeado de argon2id (~95-100+ caracteres según parámetros) no entraba
   en el `VARCHAR(100)` de la etapa 09. **Fix:** migración `V005` la
   ensancha a `VARCHAR(255)`.

## 3. Decisiones de implementación no explicitadas antes

- **Suscribir = activar directo**, sin paso de aprobación intermedio (MVP).
- **`debeCambiarPassword`** se resuelve con un endpoint dedicado
  (`PUT /auth/cambiar-password`) en vez de bloquear el resto de la API
  server-side hasta el cambio — el frontend (etapa 17/18) decide cuándo
  mostrar la pantalla forzada según ese flag en la respuesta de login.
- **Callback de OAuth responde JSON**, no un redirect a una pantalla propia
  todavía (no existe el panel admin web) — placeholder documentado para la
  etapa 17/18.
- **Storage de assets:** filesystem local (`./data/assets`, configurable),
  no S3/CDN real — evolución futura documentada, no bloqueante para el MVP.

## 4. Cómo se verificó

**20/20 tests, BUILD SUCCESS**, contra MySQL 8 real (Testcontainers):

- `SuperAdminBalnearioFlowIntegrationTest` — el flujo completo del criterio
  de aceptación: crear balneario → vincular Mercado Pago (fake) → leer
  branding default → suscribir a plan+temporada → aparece en el listado
  público → suspender (desaparece + rechaza) → reactivar balneario pero
  suspender la suscripción (sigue no operativo).
- `SuperAdminAccessControlIntegrationTest` — admin de balneario y cliente
  reciben `403` en endpoints de Super Admin; sin token, `401` (agregado un
  `AuthenticationEntryPoint` explícito: sin él, Spring Security devuelve
  `403` tanto para "sin credenciales" como para "rol insuficiente",
  contradiciendo la distinción 401/403 de la etapa 05).
- `BrandingContractIntegrationTest` — el endpoint público devuelve las 25
  claves exactas del contrato (ninguna de más, ninguna de menos); subir un
  PNG real funciona y queda accesible por URL pública; un archivo de texto
  disfrazado de `.png` se rechaza por magic bytes.
- Suite completa de la etapa 09 (auth, aislamiento multitenant, ArchUnit)
  sigue en verde: sin regresiones.
