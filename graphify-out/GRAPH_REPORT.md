# Graph Report - .  (2026-07-24)

## Corpus Check
- 2 files · ~252,590 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 268 nodes · 400 edges · 22 communities (8 shown, 14 thin omitted)
- Extraction: 96% EXTRACTED · 4% INFERRED · 0% AMBIGUOUS · INFERRED: 17 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_ADRs Multitenancy y Pagos Mercado Pago|ADRs: Multitenancy y Pagos Mercado Pago]]
- [[_COMMUNITY_Visión, Pagos MP y Objetivo de Negocio|Visión, Pagos MP y Objetivo de Negocio]]
- [[_COMMUNITY_ADRs Monolito, Tiempo Real y Theming|ADRs: Monolito, Tiempo Real y Theming]]
- [[_COMMUNITY_Módulos Backend y Eventos de Dominio|Módulos Backend y Eventos de Dominio]]
- [[_COMMUNITY_Plan de Ejecución Índice de Etapas|Plan de Ejecución: Índice de Etapas]]
- [[_COMMUNITY_Glosario de Dominio y Actores|Glosario de Dominio y Actores]]
- [[_COMMUNITY_Etapa 08 Diseño de Paneles Web|Etapa 08: Diseño de Paneles Web]]
- [[_COMMUNITY_Etapa 07 Diseño Mobile Cliente|Etapa 07: Diseño Mobile Cliente]]
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
1. `Plan de ejecución EasyBeach — Índice de etapas` - 30 edges
2. `Etapa 02 — Arquitectura general y estrategia multitenant (documento)` - 14 edges
3. `Etapa 03 — Modelo de datos MySQL (documento)` - 14 edges
4. `Etapa 07 — Diseño UX/UI app mobile (cliente)` - 14 edges
5. `Etapa 04 — Convenciones y contratos de API REST (documento)` - 13 edges
6. `Etapa 05 — Seguridad, autenticación, roles y permisos (documento)` - 13 edges
7. `ADR-001: Multitenancy — base compartida con balneario_id` - 12 edges
8. `ADR-004: Mercado Pago Checkout API — marketplace sin comisión` - 12 edges
9. `Etapa 09 — Backend fundacional` - 12 edges
10. `Etapa 19 — QA integral, seeds y datos demo` - 12 edges

## Surprising Connections (you probably didn't know these)
- `Solicitud de servicio al carpero (implementación)` --semantically_similar_to--> `Colas operativas por balneario`  [INFERRED] [semantically similar]
  docs/etapas/14-backend-carpero-promociones.md → docs/etapas/13-backend-pedidos.md
- `Bandeja de validación de estadías (construcción)` --references--> `Apertura de estadía en dos pasos (solicitud + validación carpero)`  [EXTRACTED]
  docs/etapas/17-web-panel-operativo-admin.md → docs/etapas/12-backend-estadia-activa.md
- `Reconexión Last-Event-ID + heartbeat` --semantically_similar_to--> `Cache local + revalidación ETag / offline`  [INFERRED] [semantically similar]
  docs/adr/ADR-003-tiempo-real-sse.md → docs/adr/ADR-005-theming-runtime.md
- `Endpoint group: Estadía` --shares_data_with--> `Tabla estadia`  [INFERRED]
  docs/api/openapi.yaml → docs/especificacion/03-modelo-de-datos.md
- `Endpoint group: Pedidos` --shares_data_with--> `Tabla pedido`  [INFERRED]
  docs/api/openapi.yaml → docs/especificacion/03-modelo-de-datos.md

## Hyperedges (group relationships)
- **Aislamiento multitenant en tres capas** — docs_adr_adr_001_multitenancy_base_compartida_multitenancy, docs_adr_adr_001_multitenancy_base_compartida_tenantcontext, docs_adr_adr_001_multitenancy_base_compartida_hibernate_filter, docs_adr_adr_001_multitenancy_base_compartida_archunit_tenant_test [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace** — docs_adr_adr_004_pagos_mercadopago_marketplace_checkout_api, docs_especificacion_03_modelo_de_datos_tabla_pedido_pago, docs_especificacion_03_modelo_de_datos_tabla_balneario_mp_credencial, docs_api_openapi_pagos [INFERRED 0.85]
- **Sistema de theming white-label** — docs_adr_adr_005_theming_runtime_theming_runtime, docs_especificacion_03_modelo_de_datos_tabla_configuracion_visual, docs_api_openapi_publico, docs_etapas_06_identidad_visual_design_system_contrato_tokens_v1 [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace de punta a punta** — docs_especificacion_01_vision_alcance_glosario_mercado_pago_marketplace, docs_etapas_10_backend_super_admin_vinculacion_mp_oauth, docs_etapas_13_backend_pedidos_webhook_mp_recepcion, docs_etapas_05_seguridad_roles_seguridad_pagos_mp, docs_etapas_03_modelo_de_datos_pedido_pago_tabla [INFERRED 0.90]
- **Flujo de validación de estadía por el carpero** — docs_especificacion_01_vision_alcance_glosario_validacion_estadia_carpero, docs_etapas_12_backend_estadia_activa_apertura_dos_pasos, docs_etapas_07_ux_ui_mobile_pantalla_espera_confirmacion, docs_etapas_08_ux_ui_web_paneles_bandeja_validacion_estadias, docs_etapas_04_contratos_api_bandeja_validacion_estadias [INFERRED 0.90]

## Communities (22 total, 14 thin omitted)

### Community 0 - "ADRs: Multitenancy y Pagos Mercado Pago"
Cohesion: 0.06
Nodes (50): Regla ArchUnit @TenantScoped + batería cross-tenant, Discriminador balneario_id, Opción: base por tenant (descartada), Hibernate @Filter / @TenantScoped, ADR-001: Multitenancy — base compartida con balneario_id, Opción: schema por tenant (descartada), TenantContext request-scoped, application_fee = 0 constante del servidor (+42 more)

### Community 1 - "Visión, Pagos MP y Objetivo de Negocio"
Cohesion: 0.05
Nodes (48): Mercado Pago Checkout API, EasyBeach, Mercado Pago Marketplace (application_fee=0), Vinculación OAuth cuenta MP del balneario, Objetivo rector (vender más), Actor: Operador de barra/cocina, Pago, Pedido (+40 more)

### Community 2 - "ADRs: Monolito, Tiempo Real y Theming"
Cohesion: 0.08
Nodes (33): Opción: microservicios (descartada), ADR-002: Monolito modular por capas, Reglas de dependencia entre módulos (ArchUnit), Reconexión Last-Event-ID + heartbeat, Opción: polling puro (descartada como único canal), ADR-003: Tiempo real — SSE con fallback polling, SseEmitter de Spring, Opción: WebSocket/STOMP (descartada) (+25 more)

### Community 3 - "Módulos Backend y Eventos de Dominio"
Cohesion: 0.09
Nodes (30): Eventos de dominio in-process (ApplicationEventPublisher), Endpoint group: Admin balneario, Endpoint group: Auth, Endpoint group: Servicios, Eventos de dominio (contrato interno), Módulo backend: branding, Módulo backend: catalog, Módulo backend: concierge (+22 more)

### Community 4 - "Plan de Ejecución: Índice de Etapas"
Cohesion: 0.23
Nodes (28): Plan de ejecución EasyBeach — Índice de etapas, Etapa 01 — Visión, alcance MVP y glosario de dominio, Etapa 02 — Arquitectura general y estrategia multitenant, Etapa 03 — Modelo de datos (MySQL), Etapa 04 — Convenciones y contratos de API REST, Etapa 05 — Seguridad, autenticación, roles y permisos, Etapa 06 — Identidad visual y sistema de diseño, Etapa 07 — Diseño UX/UI app mobile (cliente) (+20 more)

### Community 5 - "Glosario de Dominio y Actores"
Cohesion: 0.10
Nodes (21): Actor: Admin de balneario, Balneario (tenant), Carpero, Actor: Cliente, Estadía activa, Servicio al carpero, Ubicación, Unicidad de estadía: una por cliente+balneario (+13 more)

### Community 6 - "Etapa 08: Diseño de Paneles Web"
Cohesion: 0.14
Nodes (20): Referencia: docs/especificacion/01-vision-alcance-glosario.md, Pagos in-app vía Mercado Pago Checkout API (marketplace sin comisión), Theming white-label total, Etapa 08 — Diseño UX/UI web (paneles), ABM de menú (categorías, variantes, fotos, disponibilidad), ubicaciones, promociones, staff, Bandeja de validación de estadías, Cola de pedidos entrantes en vivo, Cola de solicitudes de servicio al carpero (+12 more)

### Community 7 - "Etapa 07: Diseño Mobile Cliente"
Cohesion: 0.10
Nodes (20): Objetivo rector: que los balnearios vendan más, Apertura de estadía validada por carpero, App mobile cliente (React Native), Carrito, pago Mercado Pago y confirmación, Cierre de estadía (resumen de consumo), Uso de tokens del design system (etapa 06), Entregable Etapa 07: prototipo de 32 pantallas (s01–s32), Estados no felices (sin conexión, cerrado, no disponible) (+12 more)

## Knowledge Gaps
- **118 isolated node(s):** `Etapa 09 — Backend fundacional`, `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)`, `Etapa 15 — Backend reportes básicos` (+113 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Etapa 07 — Diseño UX/UI app mobile (cliente)` connect `Etapa 07: Diseño Mobile Cliente` to `Visión, Pagos MP y Objetivo de Negocio`?**
  _High betweenness centrality (0.152) - this node is a cross-community bridge._
- **Why does `Etapa 16 documento` connect `Visión, Pagos MP y Objetivo de Negocio` to `Etapa 07: Diseño Mobile Cliente`?**
  _High betweenness centrality (0.140) - this node is a cross-community bridge._
- **What connects `Etapa 09 — Backend fundacional`, `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)` to the rest of the system?**
  _124 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `ADRs: Multitenancy y Pagos Mercado Pago` be split into smaller, more focused modules?**
  _Cohesion score 0.05714285714285714 - nodes in this community are weakly interconnected._
- **Should `Visión, Pagos MP y Objetivo de Negocio` be split into smaller, more focused modules?**
  _Cohesion score 0.047872340425531915 - nodes in this community are weakly interconnected._
- **Should `ADRs: Monolito, Tiempo Real y Theming` be split into smaller, more focused modules?**
  _Cohesion score 0.07765151515151515 - nodes in this community are weakly interconnected._
- **Should `Módulos Backend y Eventos de Dominio` be split into smaller, more focused modules?**
  _Cohesion score 0.08735632183908046 - nodes in this community are weakly interconnected._