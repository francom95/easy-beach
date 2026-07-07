# ADR-001 — Multitenancy: base compartida con discriminador `balneario_id`

- **Estado:** Aceptada
- **Fecha:** 2026-07-07
- **Etapa:** 02

## Contexto

EasyBeach es un SaaS multibalneario: varios balnearios (tenants) operan sobre la
misma plataforma con datos, usuarios y operación aislados entre sí. La fuga de
datos entre tenants es el peor defecto posible del producto (ver etapa 05: el
aislamiento es una propiedad de seguridad, no solo de arquitectura).

Escala asumida (supuesto documentado, sin dato real): 10–30 balnearios en el
año 1, pico de temporada de ~5.000–15.000 clientes concurrentes en toda la
plataforma y ~100–200 pedidos/minuto agregados en el peor sábado de enero.
Volumen modesto para MySQL bien indexado.

## Opciones evaluadas

**(a) Base compartida, discriminador `balneario_id` en cada tabla tenant-scoped.**
Una sola base MySQL; toda tabla de datos de balneario lleva `balneario_id` y
todos los índices calientes arrancan por esa columna.
- ✅ Operación simple: una base que respaldar, migrar y monitorear.
- ✅ Costo mínimo de infraestructura; consultas cross-tenant triviales para el
  Super Admin y reportes de plataforma.
- ✅ Alta de balneario instantánea (un INSERT, no un provisioning).
- ❌ El aislamiento es lógico: depende de que el filtro por tenant se aplique
  SIEMPRE. Requiere defensa en profundidad (ver "Mecanismo" abajo).

**(b) Schema por tenant.** Un schema MySQL por balneario, misma instancia.
- ✅ Aislamiento más fuerte a nivel de datos.
- ❌ Migraciones × N schemas, conexiones/pooling por tenant, alta de balneario
  requiere provisioning, reportes de plataforma se vuelven federados. Complejidad
  operativa desproporcionada para 10–30 tenants y un equipo chico.

**(c) Base por tenant.** Aísla también fallas y ruido, pero multiplica el costo
de infraestructura y toda la operación. Injustificable a esta escala.

## Decisión

**Opción (a): base compartida con `balneario_id`.** Las opciones (b)/(c) quedan
documentadas como evolución posible si algún día un tenant exige aislamiento
físico contractual; el diseño (todas las queries ya filtradas por tenant) no lo
impide.

## Mecanismo de aislamiento (normativo)

1. **Resolución del tenant por request:**
   - **Staff** (carpero/operador/admin): claim `balneario_id` dentro del JWT.
     Nunca de un parámetro del request.
   - **Cliente**: el cliente es global (no pertenece a un tenant). El tenant
     operativo sale del **recurso**: en endpoints públicos, del path explícito
     (`/api/v1/balnearios/{balnearioId}/menu`); en operaciones sobre una
     estadía/pedido, del registro validado en servidor (la estadía conoce su
     balneario). Jamás de un campo libre del body.
   - **Super Admin**: contexto cross-tenant explícito y auditado; no hay
     "bypass silencioso".
2. **`TenantContext` request-scoped** poblado por un filtro/interceptor al
   inicio del request; se limpia al finalizar (cuidado con thread pools).
3. **Filtro automático en persistencia:** Hibernate `@Filter` (o discriminador
   equivalente) habilitado automáticamente para toda entidad anotada
   `@TenantScoped`, usando el valor del `TenantContext`.
4. **Defensa en profundidad** — el filtro de Hibernate NO cubre `findById`
   directo ni queries nativas, así que:
   - La capa service **siempre** verifica pertenencia
     (`recurso.balnearioId == TenantContext.get()`) al cargar por id.
   - **Regla de arquitectura verificada por test (ArchUnit):** toda `@Entity`
     con columna `balneario_id` debe estar anotada `@TenantScoped`; toda query
     nativa en repositorios tenant-scoped se revisa en code review.
   - **Batería cross-tenant obligatoria** (etapas 09 y 19): cada endpoint
     tenant-scoped probado con credenciales de otro balneario debe devolver
     404/403, nunca datos.
5. **Índices:** todo índice de tabla tenant-scoped arranca por `balneario_id`
   (detalle en etapa 03).

## Consecuencias

- El costo de agregar un tenant es cero infraestructura: habilita el modelo
  comercial de alta rápida por temporada.
- El riesgo residual (olvido de filtro) queda mitigado por tres capas
  independientes: filtro automático, aserción en service y tests cross-tenant
  en CI. Un desarrollador que olvida el filtro necesita fallar las tres a la
  vez para producir una fuga.
- Los backups y restores son de la plataforma completa; restaurar "un solo
  balneario" requiere procedimiento manual (aceptado para MVP).
