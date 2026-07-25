# EasyBeach — Backend

API multibalneario (Etapa 09: backend fundacional). Java 21, Spring Boot 3.5,
Maven, MySQL 8, Flyway, JWT (RS256).

## Requisitos

- JDK 21
- Maven 3.9+
- Docker + Docker Compose (MySQL local; también usado por los tests de
  integración vía Testcontainers)

## Levantar en local

```bash
docker compose up -d          # levanta MySQL en localhost:3306
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

La API queda en `http://localhost:8080`. Health check:
`GET http://localhost:8080/actuator/health`.

En el perfil `local` no hace falta configurar claves JWT: se genera un par
RSA efímero al arrancar (ver `identity.security.JwtKeyProvider`). Esto
significa que **reiniciar la app invalida los tokens emitidos** — esperado en
desarrollo, nunca aceptable en producción (`application-prod.yml` exige
`JWT_PRIVATE_KEY_LOCATION`/`JWT_PUBLIC_KEY_LOCATION`, sin default).

## Tests

```bash
mvn test
```

Los tests de integración (`support.AbstractIntegrationTest` y sus
subclases) levantan un contenedor MySQL real vía Testcontainers — necesitan
Docker corriendo. Cubren:

- Registro/login de cliente, login de staff, refresh rotativo con detección
  de reuso (revoca la familia completa), logout.
- Acceso denegado por rol (`403`) y acceso permitido para el rol correcto.
- Aislamiento multitenant: una query sin `TenantContext` **falla**
  (`TenantContextMissingException`); con contexto, **filtra** — solo ve su
  propio balneario aunque existan filas de otros.
- Reglas de dependencia entre módulos (ADR-002) y la convención
  `@TenantScoped` (ADR-001), verificadas con ArchUnit.

## Estructura del proyecto (ADR-002: monolito modular)

```
com.easybeach
├── shared/       tenancy, error (RFC 7807), paginación, auditoría, JWT/CORS/JSON transversal
├── identity/     usuarios, roles, autenticación JWT, autorización
├── platform/     balneario (el tenant)
├── branding/     [etapa 10] configuración visual white-label
├── catalog/      [etapa 11] menú, productos, variantes
├── stay/         [etapa 12] estadía activa, ubicaciones
├── ordering/     [etapa 13] carrito, pedidos, colas operativas
├── payments/     [etapa 13] Mercado Pago (OAuth, webhook)
├── concierge/    [etapa 14] servicios al carpero
├── promotions/   [etapa 14] promociones
└── reporting/    [etapa 15] reportes y KPIs
```

Los paquetes marcados `[etapa N]` solo tienen un `package-info.java`
documentando su responsabilidad y dependencias declaradas — el código llega
en esa etapa. Un test de ArchUnit (`ModuleDependencyRulesTest`) ya hace
cumplir esas dependencias desde ahora, para que ningún acoplamiento indebido
se cuele a medida que se completan.

## Convención de multitenancy (leer antes de tocar cualquier módulo)

Toda entidad con columna `balneario_id` debe:

1. Estar anotada `@TenantScoped` (verificado por ArchUnit).
2. Declarar el filtro Hibernate: `@FilterDef`/`@Filter` sobre
   `TenantScoped.FILTER_NAME` (ver `UsuarioBalnearioRol` como referencia).

Todo método de servicio `@Transactional` que la toque debe llamar
`tenantFilterService.applyCurrentTenant()` como **primera línea** (ver
`UsuarioBalnearioRolService`). Si no hay `TenantContext` resuelto, lanza
`TenantContextMissingException` — una query sin tenant **falla**, nunca
devuelve datos de todos los balnearios.

## Autenticación

Ver `docs/especificacion/05-seguridad-roles.md` para el detalle completo.
Resumen operativo:

- `POST /api/v1/auth/registro` — alta de cliente (auto-registro).
- `POST /api/v1/auth/login/cliente` / `.../login/staff` / `.../login/super-admin`
- `POST /api/v1/auth/refresh` — rotativo; reuso de un refresh ya rotado
  revoca toda la familia de tokens.
- `POST /api/v1/auth/logout` — revoca el refresh token usado.

El access token viaja en `Authorization: Bearer <token>`. Para staff, el
claim `balneario_id` es la única fuente del tenant operativo — nunca un
parámetro de request.

## Variables de entorno (perfiles `dev`/`prod`)

| Variable | Descripción |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Conexión a MySQL |
| `JWT_PRIVATE_KEY_LOCATION` / `JWT_PUBLIC_KEY_LOCATION` | PEM del par RSA (`file:` o `classpath:`), fuera del repo |
