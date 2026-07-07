# Etapa 02 — Arquitectura general y estrategia multitenant

- **Orden:** 02
- **Modelo ejecutor:** fable
- **Tipo:** especificación (documento, sin código)
- **Depende de:** 01
- **Estado: ✅ EJECUTADA** — entregables:
  [`docs/especificacion/02-arquitectura-general.md`](../especificacion/02-arquitectura-general.md)
  (contexto C4, módulos, eventos, frontend, transversales, escala, riesgos) y
  ADRs 001–005 en [`docs/adr/`](../adr/): multitenancy base compartida,
  monolito modular, tiempo real SSE+polling, pagos Mercado Pago marketplace
  (`application_fee = 0`), theming white-label runtime.

## Objetivo

Definir la arquitectura de referencia del sistema completo y, en particular, la
decisión más estructural del proyecto: **cómo se implementa el multitenant**
(multibalneario). Es la etapa con mayor costo de equivocarse; por eso la ejecuta
el modelo más capaz.

## Alcance / Entregables

1. **Diagrama de contexto** (C4 nivel 1–2): app mobile cliente, web de paneles,
   API Spring Boot, MySQL, servicios transversales (notificaciones, storage de
   imágenes).
2. **Decisión de multitenancy documentada como ADR** con alternativas evaluadas:
   - Base compartida con `balneario_id` discriminador en cada tabla (recomendación
     esperada para el MVP: simple, barata, suficiente para la escala inicial).
   - Schema por tenant / base por tenant (descartar con argumentos, dejar puerta
     abierta como evolución).
   - Cómo se garantiza el aislamiento: resolución del tenant por request (token /
     header / subdominio), filtro obligatorio a nivel repositorio (ej. Hibernate
     `@Filter` o criterio equivalente), y qué operaciones son cross-tenant
     (solo Super Admin).
3. **Arquitectura del backend**: monolito modular por capas (controller /
   service / repository / domain), módulos propuestos (tenancy, identity,
   catalog, stay, ordering, concierge/servicios, promotions, reporting,
   platform-admin), reglas de dependencia entre módulos, manejo de eventos
   internos (para estados de pedido y futuras notificaciones).
4. **Estrategia de tiempo real** para estados de pedido: ADR corto comparando
   polling, SSE y WebSocket/STOMP; recomendación para MVP con criterio de costo
   operativo.
5. **ADR de integración de pagos (Mercado Pago, decidido en etapa 01)**:
   modelo marketplace/split payments sin comisión de plataforma — cada
   balneario cobra en su propia cuenta de MP vinculada por OAuth,
   `application_fee = 0`. El ADR debe cubrir: flujo Checkout API (creación del
   pago contra el `access_token` del balneario), recepción asíncrona del
   resultado vía **webhook**, interacción del pago con la máquina de estados
   del pedido (un pedido no pasa a `CONFIRMADO` hasta pago aprobado), custodia
   y refresh de tokens OAuth por balneario, y manejo de fallos (pago rechazado,
   webhook demorado, reembolso por cancelación del local).
6. **Arquitectura frontend**: cómo mobile y web consumen la API, dónde vive la
   configuración visual por balneario (**theming white-label total** servido
   por API: al elegir balneario, la app cliente adopta la identidad completa
   del balneario — etapa 06 define el contrato de tokens), estrategia
   offline-tolerante mínima para la playa (conectividad irregular).
7. **Decisiones transversales**: versionado de API, formato de errores,
   paginación, zonas horarias, moneda (ARS), auditoría básica, soft-delete
   vs. borrado.
8. **ADRs**: cada decisión relevante en formato ADR corto (contexto → opciones →
   decisión → consecuencias), en `docs/adr/`.

## Inputs requeridos

- Etapa 01 aprobada (glosario y alcance) — **ver el entregable en
  `docs/especificacion/01-vision-alcance-glosario.md`**, que cierra: pagos
  in-app vía Mercado Pago marketplace sin comisión, validación de estadía por
  carpero, una estadía activa por cliente **por balneario**, variantes de
  producto incluidas, y theming white-label total.
- Expectativa de escala para dimensionar (orden de magnitud: cantidad de
  balnearios año 1, clientes concurrentes pico en temporada). Si no hay dato,
  asumir y documentar el supuesto.

## Criterios de aceptación

- La estrategia multitenant especifica el mecanismo concreto de aislamiento y
  qué pasa si un desarrollador olvida filtrar por tenant (defensa en profundidad).
- Cada módulo del backend tiene responsabilidad, entidades principales y
  dependencias declaradas.
- Hay un ADR por cada decisión estructural; ninguna decisión queda implícita.
- Un desarrollador nuevo puede leer este documento y saber dónde va cada cosa.
