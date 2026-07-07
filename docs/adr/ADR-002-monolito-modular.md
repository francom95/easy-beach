# ADR-002 — Backend: monolito modular por capas (no microservicios)

- **Estado:** Aceptada
- **Fecha:** 2026-07-07
- **Etapa:** 02

## Contexto

Hay que elegir la topología del backend Spring Boot para un MVP con ~9 áreas
funcionales (tenancy, identidad, catálogo, estadía, pedidos, pagos, servicios,
promociones, reportes), un equipo chico, y una carga estacional modesta
(supuesto ADR-001: pico ~100–200 pedidos/min en toda la plataforma).

## Opciones evaluadas

**(a) Monolito modular.** Un solo deployable Spring Boot, módulos internos con
límites explícitos (paquetes por módulo, comunicación por interfaces de service
y eventos de dominio in-process).
- ✅ Un deploy, una base, una traza; transacciones ACID entre módulos cuando
  hace falta (pedido + pago + evento en una transacción).
- ✅ Velocidad de desarrollo máxima para un equipo chico; costo de infra mínimo
  (clave para el modelo SaaS estacional).
- ❌ Exige disciplina para que los módulos no se enreden → se mitiga con reglas
  de dependencia verificadas por ArchUnit.

**(b) Microservicios.** Deploys independientes por dominio.
- ❌ Transacciones distribuidas para el flujo pedido↔pago, N pipelines, N
  observabilidades, service discovery. Todo el costo, ninguno de los beneficios
  a esta escala. Descartado.

## Decisión

**Monolito modular por capas.** Módulos (detalle en el documento de
arquitectura): `platform`, `identity`, `branding`, `catalog`, `stay`,
`ordering`, `payments`, `concierge`, `promotions`, `reporting`, `shared`.

Reglas de dependencia (normativas, verificadas con ArchUnit en CI):
1. Dentro de un módulo: `controller → service → repository → domain`; nunca al
   revés.
2. Entre módulos: solo vía **interfaces públicas de service** o **eventos de
   dominio**; prohibido acceder al repositorio o a las entidades JPA de otro
   módulo.
3. `shared` no depende de nadie; todos pueden depender de `shared`.
4. `reporting` es de solo lectura: consume queries propias, no servicios de
   otros módulos (evita acoplar reportes a la lógica transaccional).

Eventos de dominio: `ApplicationEventPublisher` de Spring con
`@TransactionalEventListener(AFTER_COMMIT)` para efectos post-transacción
(notificaciones de tiempo real, side-effects de reportes). In-process en MVP;
si algún día se extrae un módulo, los eventos ya marcan las costuras.

## Consecuencias

- El flujo crítico pedido→pago→confirmación vive en una sola transacción/
  proceso: menos modos de fallo que cualquier alternativa distribuida.
- Escalar el MVP = escalar verticalmente el monolito + réplicas detrás de un
  load balancer si hiciera falta (el estado de SSE es el único punto a cuidar;
  ver ADR-003).
- Los límites de módulo son el mapa de extracción futura: si un módulo (ej.
  `payments`) necesitara aislarse, sus interfaces y eventos ya definen el
  contrato.
