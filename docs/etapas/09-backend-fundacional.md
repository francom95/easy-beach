# Etapa 09 — Backend fundacional

- **Orden:** 09
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción (primera etapa con código)
- **Depende de:** 02, 03, 04, 05
- **Estado: ✅ EJECUTADA** — código en [`backend/`](../../backend/),
  entregable en [`docs/especificacion/09-backend-fundacional.md`](../especificacion/09-backend-fundacional.md).
  13/13 tests, BUILD SUCCESS, verificado contra MySQL 8 real (Testcontainers):
  multitenancy operativo (falla sin `TenantContext`, filtra con contexto),
  autenticación JWT RS256 completa (registro/login/refresh rotativo con
  detección de reuso/logout), autorización por rol, reglas de arquitectura
  ADR-002 (ArchUnit).

## Objetivo

Levantar el esqueleto del backend Spring Boot con las piezas transversales
funcionando: estructura modular, multitenancy, autenticación y convenciones.
Todo lo que las etapas 10–15 dan por sentado se construye acá.

## Alcance / Entregables

1. **Proyecto Spring Boot** con la estructura modular de la etapa 02 (paquetes
   por módulo, capas controller/service/repository/domain), perfiles
   (`local`, `dev`, `prod`), y `README` de setup local (Docker Compose con
   MySQL).
2. **Migraciones**: herramienta elegida en etapa 03 configurada; migración
   inicial con las tablas de tenancy e identidad.
3. **Multitenancy operativo**: resolución del tenant por request según ADR,
   filtro automático en repositorios, contexto de tenant accesible en la capa
   service, y test que demuestra que una query sin tenant falla o filtra.
4. **Autenticación completa** (etapa 05): registro/login de cliente, login de
   staff, JWT access + refresh, rotación y revocación, hashing de passwords.
5. **Autorización por rol**: infraestructura (anotaciones/config por endpoint)
   con la matriz de la etapa 05 cargada para los endpoints que existan.
6. **Convenciones transversales** (etapa 04): manejo global de errores en
   formato estándar, validación de input, paginación, CORS, logging
   estructurado con tenant y request-id, zona horaria y tipos monetarios.
7. **Salud y base de observabilidad**: actuator/health, métricas básicas.
8. **CI mínima**: build + tests en cada push (GitHub Actions o equivalente).

## Inputs requeridos

- Documentos de las etapas 02–05 aprobados (son la especificación de esta etapa).

## Criterios de aceptación

- `docker compose up` + run levanta la app contra MySQL local sin pasos manuales.
- Tests de integración prueban: login de cliente y staff, refresh, acceso
  denegado por rol, y aislamiento cross-tenant (staff del balneario A no lee
  datos del B).
- El formato de error y la paginación coinciden con la spec OpenAPI.
- CI en verde.
