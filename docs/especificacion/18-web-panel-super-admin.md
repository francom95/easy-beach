# Etapa 18 — Web: panel Super Admin

- **Estado:** ejecutada y verificada en navegador real.
- **Corresponde al plan:** [docs/etapas/18-web-panel-super-admin.md](../etapas/18-web-panel-super-admin.md)
- **Depende de:** 08 (diseño), 10 (API backend Super Admin).
- **Código:** [`web/src/app/super-admin/`](../../web/src/app/super-admin/) — mismo
  proyecto Next.js de la etapa 17.

## 1. Qué se construyó

Cierra el **Hito Producto MVP**: el cuarto y último rol de staff, sobre la
misma app web de la etapa 17 (no un proyecto nuevo). Cuatro secciones:

- **Balnearios** (`/super-admin`, también sirve de "vista de plataforma"):
  tabla con estado, suscripción vigente, volumen (pedidos/facturación de la
  temporada en curso) y acciones; alta de balneario con creación del primer
  admin (contraseña temporal mostrada una sola vez, mismo patrón que
  invitar-staff de la etapa 17); suspender/activar con motivo obligatorio
  ("queda en auditoría") y advertencia explícita del efecto operativo
  inmediato; export CSV del reporte de plataforma.
- **Detalle de balneario** (`/super-admin/balnearios/[id]`): editar datos de
  contacto; listar suscripciones con cambio de estado inline; suscribir a un
  plan+temporada.
- **Planes** (`/super-admin/planes`): ABM con activo/inactivo (sin delete —
  el backend no lo tiene, por diseño: un plan usado por una suscripción
  histórica no se puede borrar).
- **Temporadas** (`/super-admin/temporadas`): alta + cambio de estado
  (PLANIFICADA → EN_CURSO → CERRADA).
- **Auditoría** (`/super-admin/auditoria`): listado paginado con filtro por
  balneario, resolviendo el nombre contra la lista de balnearios en vez de
  mostrar solo el ID.

Login: un único formulario para todo el panel de staff (sin selector de
rol) — ver decisión de arquitectura más abajo.

## 2. Decisiones de arquitectura

- **Login unificado staff + Super Admin, con fallback en dos pasos.** El
  backend expone `/auth/login/staff` y `/auth/login/super-admin` como
  endpoints separados (etapa 05 — el login discrimina por `tipo` de
  `Usuario`: STAFF vs. SUPER_ADMIN). La etapa 17 ya tenía un único
  formulario de login para CARPERO/OPERADOR/ADMIN_BALNEARIO apuntando a
  `/login/staff`; pedirle al usuario que elija "¿sos Super Admin?" de
  antemano no aporta nada. Se resolvió en `api/auth.ts` con
  `loginStaffOSuperAdmin()`: intenta `/login/staff` primero (caso común) y,
  solo si la credencial es rechazada (401), reintenta contra
  `/login/super-admin` antes de mostrar error. Esto se descubrió recién en
  la verificación en navegador — el primer intento de login con la cuenta
  semilla `superadmin@easybeach.dev` contra `/login/staff` devolvía 401 real
  (no un bug: ese endpoint filtra por `tipo=STAFF` y la cuenta es
  `tipo=SUPER_ADMIN`), lo cual confirmó que la separación de endpoints es
  intencional y no se podía resolver enviando siempre al mismo endpoint.
- **`rutaInicioPorRol()` centralizado** (`utils/rutas.ts`): las 3 llamadas
  que antes hardcodeaban `rol === 'ADMIN_BALNEARIO' ? '/admin' : '/operativo'`
  (login, redirect de `/`, guard de `RequireAuth`) se unificaron en una sola
  función con la rama `SUPER_ADMIN → '/super-admin'` agregada una vez, para
  no tener que mantener 3 copias del mismo mapeo rol→ruta en sincronía.
- **"Vista de plataforma" fusionada con el ABM de balnearios**, no una
  pantalla aparte: el reporte de plataforma de la etapa 15
  (`GET /super-admin/reportes/plataforma`, cross-tenant, el único que cruza
  balnearios) se pisa por `balnearioId` con la lista paginada de balnearios
  en la misma tabla — evita una segunda pantalla que solo repetiría los
  mismos nombres de balneario.
- **`suscribir`/`cambiar estado de suscripción` viven en el detalle del
  balneario**, no en una pantalla de "Planes y temporadas" combinada: el
  endpoint es `POST /super-admin/balnearios/{id}/suscripciones`
  (scoped a un balneario), así que el flujo natural es "entrar al balneario
  → suscribirlo". Planes y Temporadas siguen siendo su propio ABM en el nav
  (igual que el mockup de la etapa 08), pero sin la acción de suscribir ahí.

## 3. Adaptaciones documentadas (mockup vs. backend real)

- **Columna "Cobros" (✓/✕ MP) del mockup no se construyó.**
  `AdminMercadoPagoController` (vinculación OAuth de Mercado Pago) es
  `@PreAuthorize("hasRole('ADMIN_BALNEARIO')")` y de solo lectura sobre el
  propio balneario — no existe ningún endpoint que le permita a Super Admin
  ver el estado de vinculación MP de otro balneario. Se reemplazó esa
  columna por pedidos/facturación de la temporada en curso (dato real del
  reporte de plataforma), que cumple el mismo propósito de "actividad de
  cobro" sin inventar un booleano que el backend no puede respaldar.
- **La advertencia de "Suspender" no promete el cascade de estadías.** El
  mockup describe que al suspender, "las estadías abiertas pasan a
  CERRADA_POR_SISTEMA" y "los pedidos ya pagados se pueden seguir
  despachando durante 2 horas". `BalnearioService.suspender()` (etapa 10)
  solo cambia `estado` — el comentario en el propio código lo dice
  explícitamente: la política sobre estadías/pedidos en curso al suspender
  está pensada para implementarse cuando exista la entidad `Estadia`
  (etapa 12), y nunca se implementó ahí tampoco (`CERRADA_POR_SISTEMA` es un
  valor de enum real sin ningún job que lo dispare — deuda ya señalada en el
  entregable de la etapa 16). El copy del modal de confirmación en el panel
  se ajustó para decir solo lo que es cierto hoy: deja de ser operativo de
  inmediato (bloquea estadías/pedidos *nuevos*), sin afirmar que lo que ya
  estaba en curso se cierra solo.
- **Auditoría sin selector de "usuario actor" con nombre.** `AuditoriaResponse`
  solo trae `actorUsuarioId` (no el nombre) — se muestra el ID crudo en esa
  columna en vez de resolver un nombre que requeriría un fetch adicional por
  fila sin un endpoint de lookup masivo de usuarios.

## 4. Cómo se verificó

Navegador real (Chrome vía MCP), backend levantado con
`mvn spring-boot:run -Dspring-boot.run.profiles=local` + MySQL por
`docker compose` (perfil `local`, mismo flujo manual de la etapa 17).

- Login con la cuenta semilla `superadmin@easybeach.dev` — confirmó el
  fallback `/login/staff` → `/login/super-admin` en vivo (401 real seguido
  de 200 real, no simulado).
- Alta de balneario de punta a punta: creado `balneario-verificacion` con
  su primer admin, contraseña temporal mostrada, aparece en la tabla como
  `ACTIVO` / `No operativo` (correcto: sin suscripción todavía).
- Suspender con motivo → estado pasa a `SUSPENDIDO`, KPI "Balnearios
  activos" se actualiza, acción cambia a "Reactivar"; Reactivar con motivo
  → vuelve a `ACTIVO`/`Operativo`. Ambos motivos verificados en
  `auditoria_plataforma` vía consulta directa a MySQL.
- Suscripciones: se ve la suscripción existente de datos de prueba con su
  estado y el selector de plan/temporada para suscribir queda funcional.
- Planes, Temporadas y Auditoría (con filtro por balneario) verificados con
  datos reales pre-existentes de sesiones de prueba anteriores.
- RBAC en ambas direcciones: un `ADMIN_BALNEARIO` logueado que navega a
  `/super-admin` es redirigido de vuelta a `/admin` por `RequireAuth`
  (y viceversa ya estaba cubierto desde la etapa 17).
- Export CSV de plataforma: `GET .../reportes/plataforma/csv` respondió 200.
- `tsc --noEmit` y `eslint .` limpios sobre todo `web/`.

**Bug real encontrado y corregido durante la verificación:** al
suspender/activar un balneario, el KPI "Balnearios activos" no se
actualizaba hasta recargar la página — la mutación solo invalidaba la
query de la lista de balnearios, no la del reporte de plataforma (de donde
sale ese KPI). Corregido invalidando ambas queries en el `onSuccess` de la
mutación ([`page.tsx`](../../web/src/app/super-admin/page.tsx)).

## 5. Sin cambios de backend

A diferencia de las etapas 16 y 17, esta etapa no requirió ningún cambio en
`backend/` — los 5 controladores Super Admin (`SuperAdminBalnearioController`,
`SuperAdminPlanController`, `SuperAdminTemporadaController`,
`SuperAdminSuscripcionController`, `SuperAdminAuditoriaController`) y el
reporte de plataforma ya existían completos desde la etapa 10/15. El único
gap real encontrado (login unificado) se resolvió enteramente del lado
del cliente.

## 6. Deuda explícita (heredada, no introducida acá)

- `CERRADA_POR_SISTEMA` sigue sin job que lo dispare (etapa 16/17/18 lo
  documentan las tres, ninguna lo implementó — está fuera de alcance de las
  tres).
- No hay endpoint para que Super Admin vea el estado de vinculación MP de
  un balneario ajeno — si se necesita, es un endpoint nuevo de solo lectura
  en `payments`, no algo que se pueda resolver en el cliente.
