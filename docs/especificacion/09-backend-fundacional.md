# Etapa 09 — Backend fundacional

- **Estado:** ejecutada — primer entregable con código. Insumo directo de las
  etapas 10–15 (todos los módulos backend restantes se construyen sobre esta
  base).
- **Corresponde al plan:** [docs/etapas/09-backend-fundacional.md](../etapas/09-backend-fundacional.md)
- **Depende de:** [02 (arquitectura)](02-arquitectura-general.md),
  [03 (modelo de datos)](03-modelo-de-datos.md),
  [04 (contratos de API)](04-contratos-api.md),
  [05 (seguridad)](05-seguridad-roles.md).
- **Código:** [`backend/`](../../backend/) (Java 21, Spring Boot 3.5.16, Maven, MySQL 8, Flyway, JWT RS256).

## 1. Qué se construyó

### Proyecto y estructura modular (ADR-002)

Maven, `com.easybeach` como paquete base, 11 módulos con `package-info.java`
documentando responsabilidad y dependencias declaradas. Solo `shared`,
`identity` y `platform` tienen código real en esta etapa; el resto
(`branding`, `catalog`, `stay`, `ordering`, `payments`, `concierge`,
`promotions`, `reporting`) queda con su paquete y su contrato de
dependencias ya verificado por ArchUnit, listo para que las etapas 10–15
construyan sin negociar de nuevo los límites entre módulos.

### Multitenancy operativo (ADR-001)

- `TenantContext` (ThreadLocal) + `@TenantScoped` (anotación marcadora,
  verificada por ArchUnit: toda `@Entity` con campo `balnearioId` debe
  llevarla) + `@FilterDef`/`@Filter` de Hibernate.
- **`TenantFilterService.applyCurrentTenant()`**: convención obligatoria —
  primera línea de todo service `@Transactional` que toque una entidad
  tenant-scoped. Si no hay `TenantContext` resuelto, **lanza excepción**
  (`TenantContextMissingException`) en vez de dejar pasar una query sin
  filtrar. "Una query sin tenant falla" es literal, no una figura retórica.
- Primera entidad tenant-scoped: `UsuarioBalnearioRol`. Demostrado con un
  service (`UsuarioBalnearioRolService.listarMiembrosDelBalnearioActual()`)
  que hace `repository.findAll()` **sin** ningún `WHERE balneario_id` en el
  código — el aislamiento lo garantiza el filtro Hibernate, no la query.

### Autenticación completa (etapa 05 §1)

- JWT **RS256** (par de claves asimétrico; efímero en local/dev, PEM
  obligatorio vía variables de entorno en prod).
- Claims exactos de la especificación: `sub` (ULID), `tipo`, `rol`,
  `balneario_id` (solo staff), `iat`/`exp`/`jti`.
- Refresh token **opaco**, hash persistido (`sesion_refresh`), **rotativo**:
  cada uso rota a un token nuevo dentro de la misma familia; el reuso de un
  token ya rotado revoca **toda la familia** (posible robo). La revocación
  corre en su propia transacción (`REQUIRES_NEW`, `SesionRefreshRevocationService`)
  para que sobreviva aunque el request que la disparó termine en excepción.
- Endpoints: `POST /api/v1/auth/{registro,login/cliente,login/staff,login/super-admin,refresh,logout}`.
- Revocación por baja de usuario verificada en cada request
  (`JwtAuthenticationFilter` rechaza tokens de usuarios `BAJA`).

### Autorización por rol (etapa 05 §2)

`@PreAuthorize` declarativo por endpoint (`StaffController` exige
`CARPERO`/`OPERADOR`/`ADMIN_BALNEARIO`) + la convención de que la capa
service **siempre** debe verificar ownership/tenant además — la anotación
sola no alcanza.

### Convenciones transversales (etapa 04)

- **Errores RFC 7807** (`GlobalExceptionHandler` + `ProblemDetail` nativo de
  Spring 6): `type/title/status/detail/instance` + `code` de negocio estable
  + `errors[]` para validaciones de campo.
- **Paginación** offset (`PageResponse<T>`, `page`/`size`/`totalElements`/`totalPages`, tope 100).
- **Dinero**: `BigDecimal` serializado como string decimal en JSON (nunca number).
- **CORS**, **logging estructurado** con `requestId` (MDC) + `balnearioId`
  (MDC, poblado por `JwtAuthenticationFilter`).
- Auditoría base: `created_at`/`updated_at` (`Auditable`, JPA Auditing en UTC).

### Migraciones (Flyway)

`V001__tenancy_baseline.sql` (balneario, configuracion_visual, plan,
temporada, suscripcion_temporada) y `V002__identity.sql` (usuario, rol +
seed, usuario_balneario_rol, `sesion_refresh` — infraestructura de auth no
anticipada como tabla propia en el plan original de la etapa 03).

### Observabilidad y CI

Actuator (`health`, `info`, `metrics`), workflow de GitHub Actions
(`.github/workflows/backend-ci.yml`) que corre `mvn verify` sobre
`ubuntu-latest` (Docker preinstalado, sin pasos extra para Testcontainers).

## 2. Cómo se verificó (no solo "compila")

13 tests, **BUILD SUCCESS**, contra un **MySQL 8 real** vía Testcontainers
(no H2/mocks) — patrón singleton (contenedor arrancado una vez, compartido
entre clases de test; usar `@Container` lo para al final de cada clase y
rompe las siguientes, documentado en `AbstractIntegrationTest`):

- `EasyBeachApplicationTests` — el contexto completo levanta contra Flyway real.
- `AuthFlowIntegrationTest` (6): registro+login cliente, login staff con
  `balneario_id`/`rol` correctos, refresh rota el token, **reuso de un
  refresh ya rotado revoca la familia completa** (el token recién emitido en
  la rotación también deja de servir), logout revoca, acceso denegado por
  rol (`403`) para cliente en endpoint de staff.
- `TenantIsolationIntegrationTest` (2): query sin `TenantContext` **lanza
  excepción**; con contexto, un balneario solo ve sus propios miembros
  aunque existan filas de otros balnearios en la misma tabla.
- `ModuleDependencyRulesTest` (ArchUnit, 2): dependencias entre módulos
  ajustadas a ADR-002; toda entidad con `balnearioId` lleva `@TenantScoped`.
- `UlidGeneratorTest` (2): unitario, formato Crockford Base32 de 26 caracteres.

## 3. Bugs reales encontrados y corregidos durante la verificación

Documentados porque son trampas conocidas de Spring/Testcontainers, no
errores triviales — vale la pena que las etapas 10-15 no los repitan:

1. **Filtro JWT como `@Bean` rompía el arranque.** Exponer
   `JwtAuthenticationFilter` como `@Bean` de tipo `Filter` hace que Spring
   Boot lo auto-registre vía `ServletContextInitializerBeans`, que corre
   *antes* de que el `EntityManagerFactory` esté listo — al depender de
   `UsuarioRepository`, fallaba con
   `Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'`.
   **Fix:** construirlo a mano dentro de `securityFilterChain(...)`, sin
   exponerlo como bean.
2. **Mismatch de tipo de columna en 3 campos ULID/hash.** Las migraciones
   declaran `CHAR(26)`/`CHAR(64)` (fijo, apropiado para IDs de largo
   constante); las entidades JPA, sin `columnDefinition` explícito, mapean
   `String` a `VARCHAR` por default — `ddl-auto=validate` lo rechaza.
   **Fix:** `columnDefinition = "CHAR(N)"` en `Usuario.publicId`,
   `SesionRefresh.familiaId`, `SesionRefresh.tokenHash`.
3. **`@Container` de Testcontainers paraba el MySQL entre clases de test.**
   El contenedor está declarado `static` en una superclase compartida por
   varias clases de test; `@Container` le da a JUnit control del ciclo de
   vida y lo **detiene al terminar cada clase**, dejando a las siguientes
   sin conexión (`Communications link failure`). **Fix:** patrón singleton
   oficial de Testcontainers — arrancarlo a mano en un bloque estático, sin
   `@Container`/`@Testcontainers`.
4. **Rollback se llevaba puesta la revocación que queríamos preservar.**
   `AuthService.refresh()` revocaba la familia completa yـ**en la misma
   transacción**ـ lanzaba `ApiException` para señalizar el reuso: Spring
   hace rollback de toda la transacción ante cualquier `RuntimeException`,
   así que la revocación (que sí queríamos commitear) se deshacía. **Fix:**
   extraer la revocación a `SesionRefreshRevocationService`, un bean
   separado con `@Transactional(propagation = REQUIRES_NEW)` — tiene que
   ser un bean aparte porque la autoinvocación (`this.metodo()` dentro de la
   misma clase) ignora el proxy de Spring y `REQUIRES_NEW` no tendría efecto
   si fuera un método privado de `AuthService`.
5. **ArchUnit marcaba falsos positivos sobre el propio código de test.** El
   importer escaneaba también `src/test/java`, y los tests de integración
   legítimamente cruzan paquetes de módulo para armar fixtures. **Fix:**
   `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`.

## 4. Decisiones de implementación no explicitadas antes (documentadas acá)

- **Maven** (no Gradle) — sin mvnw: se documenta JDK 21 + Maven 3.9+ como
  prerequisito en el README, igual que Node lo es para un proyecto JS.
- **Spring Boot 3.5.16** (última de la línea 3.x al momento de ejecutar esta
  etapa) en vez de la 4.x recién disponible: preferí la superficie de API
  que conozco con certeza para poder verificar todo con tests reales, en vez
  de arriesgar una migración mayor sin poder validar cada detalle.
- **Autenticación manual** (comparación directa de `passwordHash` con
  `PasswordEncoder`) en vez del flujo estándar
  `AuthenticationManager`/`UserDetailsService` de Spring Security: el matiz
  cliente/staff/super-admin y la resolución de `balneario_id` no encajan
  bien en el flujo estándar pensado para un único tipo de usuario.
- **Login de staff toma la primera membresía** si un usuario tuviera más de
  una fila en `usuario_balneario_rol` (caso no contemplado como del MVP).
  Simplificación documentada, no bloqueante.

## 5. Deuda explícita para etapas siguientes

- El endpoint `/api/v1/staff/miembros` es una demostración mínima del
  mecanismo de tenant, no un ABM real — los ABMs de staff llegan en las
  etapas 10-15, ya sobre esta misma convención.
- No hay todavía blacklist de `jti` para revocación instantánea de access
  tokens individuales (la revocación por baja sí es instantánea, vía chequeo
  de `usuario.estado` en cada request); si hiciera falta revocar un access
  token puntual antes de su expiración natural, se resuelve con una cache
  corta de `jti`s revocados — no requerido por ningún criterio de esta etapa.
