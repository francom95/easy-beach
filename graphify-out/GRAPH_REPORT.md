# Graph Report - .  (2026-07-23)

## Corpus Check
- 24 files · ~157,597 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 217 nodes · 281 edges · 22 communities (8 shown, 14 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 14 edges (avg confidence: 0.86)
- Token cost: 0 input · 196,506 output

## Community Hubs (Navigation)
- [[_COMMUNITY_ADRs de Arquitectura (SSE, Monolito, Theming)|ADRs de Arquitectura (SSE, Monolito, Theming)]]
- [[_COMMUNITY_Módulos Backend y Eventos de Dominio|Módulos Backend y Eventos de Dominio]]
- [[_COMMUNITY_Multitenancy y Modelo de Datos|Multitenancy y Modelo de Datos]]
- [[_COMMUNITY_Visión, Pagos MP y Objetivo de Negocio|Visión, Pagos MP y Objetivo de Negocio]]
- [[_COMMUNITY_Estadía Activa y Dominio Operativo|Estadía Activa y Dominio Operativo]]
- [[_COMMUNITY_ADR Pagos Mercado Pago (detalle técnico)|ADR Pagos Mercado Pago (detalle técnico)]]
- [[_COMMUNITY_Backend de Pedidos y Vinculación MP|Backend de Pedidos y Vinculación MP]]
- [[_COMMUNITY_Paneles Web (construcción)|Paneles Web (construcción)]]
- [[_COMMUNITY_Alcance MVP y Promociones|Alcance MVP y Promociones]]
- [[_COMMUNITY_Super Admin y Temporadas|Super Admin y Temporadas]]
- [[_COMMUNITY_Backend Fundacional e Infraestructura|Backend Fundacional e Infraestructura]]
- [[_COMMUNITY_Etapas Carpero y Reportes|Etapas Carpero y Reportes]]
- [[_COMMUNITY_Graphify Workflow|Graphify Workflow]]
- [[_COMMUNITY_API Operativo|API Operativo]]
- [[_COMMUNITY_Tabla Auditoría|Tabla Auditoría]]
- [[_COMMUNITY_Tabla Historial Ubicación|Tabla Historial Ubicación]]
- [[_COMMUNITY_Tabla Pedido-Promoción|Tabla Pedido-Promoción]]
- [[_COMMUNITY_Tabla Alcance de Promoción|Tabla Alcance de Promoción]]
- [[_COMMUNITY_Tabla Combo Item|Tabla Combo Item]]
- [[_COMMUNITY_Tabla Usuario-Balneario-Rol|Tabla Usuario-Balneario-Rol]]
- [[_COMMUNITY_Promociones Básicas|Promociones Básicas]]
- [[_COMMUNITY_Estacionalidad de Infraestructura|Estacionalidad de Infraestructura]]

## God Nodes (most connected - your core abstractions)
1. `Etapa 02 — Arquitectura general y estrategia multitenant (documento)` - 14 edges
2. `Etapa 03 — Modelo de datos MySQL (documento)` - 14 edges
3. `Etapa 04 — Convenciones y contratos de API REST (documento)` - 13 edges
4. `Etapa 05 — Seguridad, autenticación, roles y permisos (documento)` - 13 edges
5. `ADR-001: Multitenancy — base compartida con balneario_id` - 12 edges
6. `ADR-004: Mercado Pago Checkout API — marketplace sin comisión` - 12 edges
7. `ADR-003: Tiempo real — SSE con fallback polling` - 10 edges
8. `ADR-005: Theming white-label runtime servido por API` - 10 edges
9. `Módulo backend: platform` - 10 edges
10. `Etapa 13 documento` - 9 edges

## Surprising Connections (you probably didn't know these)
- `Solicitud de servicio al carpero (implementación)` --semantically_similar_to--> `Colas operativas por balneario`  [INFERRED] [semantically similar]
  docs/etapas/14-backend-carpero-promociones.md → docs/etapas/13-backend-pedidos.md
- `Pago vía Mercado Pago (sandbox) - testing` --references--> `Mercado Pago Marketplace (application_fee=0)`  [EXTRACTED]
  docs/etapas/19-qa-seeds-demo.md → docs/especificacion/01-vision-alcance-glosario.md
- `Bandeja de validación de estadías (UI)` --references--> `Validación de apertura de estadía por el carpero`  [EXTRACTED]
  docs/etapas/08-ux-ui-web-paneles.md → docs/especificacion/01-vision-alcance-glosario.md
- `Apertura de estadía en dos pasos (solicitud + validación carpero)` --semantically_similar_to--> `Pantalla de espera de confirmación (PENDIENTE_VALIDACION)`  [INFERRED] [semantically similar]
  docs/etapas/12-backend-estadia-activa.md → docs/etapas/07-ux-ui-mobile.md
- `Creación de pedido desde carrito` --references--> `Apertura de estadía en dos pasos (solicitud + validación carpero)`  [EXTRACTED]
  docs/etapas/13-backend-pedidos.md → docs/etapas/12-backend-estadia-activa.md

## Hyperedges (group relationships)
- **Aislamiento multitenant en tres capas** — docs_adr_adr_001_multitenancy_base_compartida_multitenancy, docs_adr_adr_001_multitenancy_base_compartida_tenantcontext, docs_adr_adr_001_multitenancy_base_compartida_hibernate_filter, docs_adr_adr_001_multitenancy_base_compartida_archunit_tenant_test [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace** — docs_adr_adr_004_pagos_mercadopago_marketplace_checkout_api, docs_especificacion_03_modelo_de_datos_tabla_pedido_pago, docs_especificacion_03_modelo_de_datos_tabla_balneario_mp_credencial, docs_api_openapi_pagos [INFERRED 0.85]
- **Sistema de theming white-label** — docs_adr_adr_005_theming_runtime_theming_runtime, docs_especificacion_03_modelo_de_datos_tabla_configuracion_visual, docs_api_openapi_publico, docs_etapas_06_identidad_visual_design_system_contrato_tokens_v1 [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace de punta a punta** — docs_especificacion_01_vision_alcance_glosario_mercado_pago_marketplace, docs_etapas_10_backend_super_admin_vinculacion_mp_oauth, docs_etapas_13_backend_pedidos_webhook_mp_recepcion, docs_etapas_05_seguridad_roles_seguridad_pagos_mp, docs_etapas_03_modelo_de_datos_pedido_pago_tabla [INFERRED 0.90]
- **Flujo de validación de estadía por el carpero** — docs_especificacion_01_vision_alcance_glosario_validacion_estadia_carpero, docs_etapas_12_backend_estadia_activa_apertura_dos_pasos, docs_etapas_07_ux_ui_mobile_pantalla_espera_confirmacion, docs_etapas_08_ux_ui_web_paneles_bandeja_validacion_estadias, docs_etapas_04_contratos_api_bandeja_validacion_estadias [INFERRED 0.90]

## Communities (22 total, 14 thin omitted)

### Community 0 - "ADRs de Arquitectura (SSE, Monolito, Theming)"
Cohesion: 0.08
Nodes (33): Opción: microservicios (descartada), ADR-002: Monolito modular por capas, Reglas de dependencia entre módulos (ArchUnit), Reconexión Last-Event-ID + heartbeat, Opción: polling puro (descartada como único canal), ADR-003: Tiempo real — SSE con fallback polling, SseEmitter de Spring, Opción: WebSocket/STOMP (descartada) (+25 more)

### Community 1 - "Módulos Backend y Eventos de Dominio"
Cohesion: 0.08
Nodes (32): Eventos de dominio in-process (ApplicationEventPublisher), Endpoint group: Admin balneario, Endpoint group: Auth, Endpoint group: Servicios, Endpoint group: SuperAdmin, Eventos de dominio (contrato interno), Módulo backend: branding, Módulo backend: catalog (+24 more)

### Community 2 - "Multitenancy y Modelo de Datos"
Cohesion: 0.09
Nodes (30): Regla ArchUnit @TenantScoped + batería cross-tenant, Discriminador balneario_id, Opción: base por tenant (descartada), Hibernate @Filter / @TenantScoped, ADR-001: Multitenancy — base compartida con balneario_id, Opción: schema por tenant (descartada), TenantContext request-scoped, Máquina de estados pago↔pedido (+22 more)

### Community 3 - "Visión, Pagos MP y Objetivo de Negocio"
Cohesion: 0.08
Nodes (27): Mercado Pago Checkout API, EasyBeach, Mercado Pago Marketplace (application_fee=0), Objetivo rector (vender más), Actor: Operador de barra/cocina, Pago, Pedido, Producto con variantes (+19 more)

### Community 4 - "Estadía Activa y Dominio Operativo"
Cohesion: 0.11
Nodes (21): Actor: Admin de balneario, Balneario (tenant), Carpero, Actor: Cliente, Estadía activa, Servicio al carpero, Ubicación, Unicidad de estadía: una por cliente+balneario (+13 more)

### Community 5 - "ADR Pagos Mercado Pago (detalle técnico)"
Cohesion: 0.12
Nodes (20): application_fee = 0 constante del servidor, ADR-004: Mercado Pago Checkout API — marketplace sin comisión, Idempotencia doble (pedido + webhook), OAuth vinculación MP del balneario, Job de reconciliación de pagos pendientes, Webhook payment.updated + verificación server-to-server, Endpoint group: Estadía, Endpoint group: Pagos (+12 more)

### Community 6 - "Backend de Pedidos y Vinculación MP"
Cohesion: 0.12
Nodes (18): Vinculación OAuth cuenta MP del balneario, Configuración visual con preview en vivo, Estado de vinculación con Mercado Pago (UI), Panel admin de balneario, Vinculación de Mercado Pago por balneario (OAuth) - backend, Aplicación de promociones en el pedido, Colas operativas por balneario, Creación de pedido desde carrito (+10 more)

### Community 7 - "Paneles Web (construcción)"
Cohesion: 0.11
Nodes (18): Bandeja de validación de estadías (UI), Etapa 08 documento, Panel operativo (barra/cocina/carpero), Panel Super Admin, Bandeja de validación de estadías (construcción), Etapa 17 documento, Panel admin de balneario (construcción), Panel operativo (construcción) (+10 more)

## Knowledge Gaps
- **97 isolated node(s):** `Etapa 09 — Backend fundacional`, `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)`, `Etapa 15 — Backend reportes básicos` (+92 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Etapa 02 — Arquitectura general y estrategia multitenant (documento)` connect `ADRs de Arquitectura (SSE, Monolito, Theming)` to `Multitenancy y Modelo de Datos`, `ADR Pagos Mercado Pago (detalle técnico)`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Why does `Etapa 03 — Modelo de datos MySQL (documento)` connect `Multitenancy y Modelo de Datos` to `ADRs de Arquitectura (SSE, Monolito, Theming)`, `ADR Pagos Mercado Pago (detalle técnico)`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **Why does `Etapa 04 — Convenciones y contratos de API REST (documento)` connect `Multitenancy y Modelo de Datos` to `ADRs de Arquitectura (SSE, Monolito, Theming)`, `Módulos Backend y Eventos de Dominio`, `ADR Pagos Mercado Pago (detalle técnico)`?**
  _High betweenness centrality (0.049) - this node is a cross-community bridge._
- **What connects `Etapa 09 — Backend fundacional`, `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)` to the rest of the system?**
  _103 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ADRs de Arquitectura (SSE, Monolito, Theming)` be split into smaller, more focused modules?**
  _Cohesion score 0.07765151515151515 - nodes in this community are weakly interconnected._
- **Should `Módulos Backend y Eventos de Dominio` be split into smaller, more focused modules?**
  _Cohesion score 0.08064516129032258 - nodes in this community are weakly interconnected._
- **Should `Multitenancy y Modelo de Datos` be split into smaller, more focused modules?**
  _Cohesion score 0.09195402298850575 - nodes in this community are weakly interconnected._