# Graph Report - .  (2026-07-07)

## Corpus Check
- 19 files · ~12,009 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 157 nodes · 204 edges · 16 communities (11 shown, 5 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 4 edges (avg confidence: 0.88)
- Token cost: 0 input · 123,966 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Estadía Activa y Dominio Operativo|Estadía Activa y Dominio Operativo]]
- [[_COMMUNITY_Modelo de Datos y Pedidos|Modelo de Datos y Pedidos]]
- [[_COMMUNITY_Visión, Stack y Objetivo de Negocio|Visión, Stack y Objetivo de Negocio]]
- [[_COMMUNITY_Pagos Mercado Pago Marketplace|Pagos Mercado Pago Marketplace]]
- [[_COMMUNITY_API, Seguridad y Multitenancy|API, Seguridad y Multitenancy]]
- [[_COMMUNITY_Theming White-Label por Balneario|Theming White-Label por Balneario]]
- [[_COMMUNITY_Paneles Web (construcción) y QA|Paneles Web (construcción) y QA]]
- [[_COMMUNITY_Etapas de Especificación y Diseño|Etapas de Especificación y Diseño]]
- [[_COMMUNITY_Etapas de Construcción BackendDeploy|Etapas de Construcción Backend/Deploy]]
- [[_COMMUNITY_UX Mobile y Design System|UX Mobile y Design System]]
- [[_COMMUNITY_Diseño UX de Paneles|Diseño UX de Paneles]]
- [[_COMMUNITY_Alcance MVP y Promociones|Alcance MVP y Promociones]]
- [[_COMMUNITY_Super Admin y Temporadas|Super Admin y Temporadas]]
- [[_COMMUNITY_Etapas Carpero y Reportes|Etapas Carpero y Reportes]]
- [[_COMMUNITY_Promociones Básicas|Promociones Básicas]]
- [[_COMMUNITY_Infraestructura Estacional|Infraestructura Estacional]]

## God Nodes (most connected - your core abstractions)
1. `Etapa 03 documento` - 13 edges
2. `Validación de apertura de estadía por el carpero` - 10 edges
3. `Mercado Pago Marketplace (application_fee=0)` - 9 edges
4. `Etapa 13 documento` - 9 edges
5. `Etapa 04 documento` - 7 edges
6. `Etapa 07 documento` - 7 edges
7. `Etapa 10 documento` - 7 edges
8. `Etapa 12 documento` - 7 edges
9. `Etapa 16 documento` - 7 edges
10. `Etapa 19 documento` - 7 edges

## Surprising Connections (you probably didn't know these)
- `graphify` --conceptually_related_to--> `Plan de ejecución por etapas`  [AMBIGUOUS]
  CLAUDE.md → docs/etapas/00-indice.md
- `Solicitud de servicio al carpero (implementación)` --semantically_similar_to--> `Colas operativas por balneario`  [INFERRED] [semantically similar]
  docs/etapas/14-backend-carpero-promociones.md → docs/etapas/13-backend-pedidos.md
- `Tabla pedido_pago` --implements--> `Mercado Pago Marketplace (application_fee=0)`  [EXTRACTED]
  docs/etapas/03-modelo-de-datos.md → docs/especificacion/01-vision-alcance-glosario.md
- `Pago vía Mercado Pago (sandbox) - testing` --references--> `Mercado Pago Marketplace (application_fee=0)`  [EXTRACTED]
  docs/etapas/19-qa-seeds-demo.md → docs/especificacion/01-vision-alcance-glosario.md
- `Estado de vinculación con Mercado Pago (UI)` --references--> `Vinculación OAuth cuenta MP del balneario`  [EXTRACTED]
  docs/etapas/08-ux-ui-web-paneles.md → docs/especificacion/01-vision-alcance-glosario.md

## Hyperedges (group relationships)
- **Flujo de pago Mercado Pago marketplace de punta a punta** — docs_especificacion_01_vision_alcance_glosario_mercado_pago_marketplace, docs_etapas_10_backend_super_admin_vinculacion_mp_oauth, docs_etapas_13_backend_pedidos_webhook_mp_recepcion, docs_etapas_05_seguridad_roles_seguridad_pagos_mp, docs_etapas_03_modelo_de_datos_pedido_pago_tabla [INFERRED 0.90]
- **Pipeline de theming white-label de diseño a runtime** — docs_etapas_06_identidad_visual_design_system_theming_white_label, docs_etapas_06_identidad_visual_design_system_tokens_diseno, docs_etapas_10_backend_super_admin_endpoint_branding_publico, docs_etapas_16_mobile_app_cliente_theming_dinamico_runtime, docs_etapas_17_web_panel_operativo_admin_theme_preview_vivo [INFERRED 0.90]
- **Flujo de validación de estadía por el carpero** — docs_especificacion_01_vision_alcance_glosario_validacion_estadia_carpero, docs_etapas_12_backend_estadia_activa_apertura_dos_pasos, docs_etapas_07_ux_ui_mobile_pantalla_espera_confirmacion, docs_etapas_08_ux_ui_web_paneles_bandeja_validacion_estadias, docs_etapas_04_contratos_api_bandeja_validacion_estadias [INFERRED 0.90]

## Communities (16 total, 5 thin omitted)

### Community 0 - "Estadía Activa y Dominio Operativo"
Cohesion: 0.10
Nodes (23): Actor: Admin de balneario, Balneario (tenant), Carpero, Actor: Cliente, Estadía activa, Servicio al carpero, Ubicación, Unicidad de estadía: una por cliente+balneario (+15 more)

### Community 1 - "Modelo de Datos y Pedidos"
Cohesion: 0.13
Nodes (17): Tabla balneario, Tabla configuracion_visual, Etapa 03 documento, Máquina de estados: pedido, Tabla pedido_pago, Tabla pedido / pedido_item, Tabla producto_variante, Tabla promocion (+9 more)

### Community 2 - "Visión, Stack y Objetivo de Negocio"
Cohesion: 0.12
Nodes (16): graphify, EasyBeach, Objetivo rector (vender más), Actor: Operador de barra/cocina, Pago, Pedido, Producto con variantes, Plan de ejecución por etapas (+8 more)

### Community 3 - "Pagos Mercado Pago Marketplace"
Cohesion: 0.18
Nodes (15): Mercado Pago Marketplace (application_fee=0), Vinculación OAuth cuenta MP del balneario, Webhook de Mercado Pago, ADR: integración de pagos Mercado Pago, Tabla balneario_mp_credencial, Idempotencia en creación de pedidos, Seguridad de pagos Mercado Pago (webhook + OAuth custodia), Vinculación de Mercado Pago por balneario (OAuth) - backend (+7 more)

### Community 4 - "API, Seguridad y Multitenancy"
Cohesion: 0.14
Nodes (15): ADR: estrategia multitenancy (balneario_id discriminador), Bandeja de validación de estadías (endpoint staff), Endpoints de estadía (solicitar, consultar, cerrar), Endpoints de pago Mercado Pago (creación + webhook), Etapa 04 documento, Matriz endpoint → rol, Especificación OpenAPI 3 (openapi.yaml), Aislamiento multitenant como control de seguridad (+7 more)

### Community 5 - "Theming White-Label por Balneario"
Cohesion: 0.21
Nodes (13): Theming White-Label Total, Configuración visual servida por API (theming), Sistema de theming white-label por balneario, Tokens de diseño (color.primary, typography.family, asset.splash), Transición de marca (splash al elegir balneario), Onboarding y selección de balneario, ABM de balnearios, Endpoints de configuración visual (theme completo) (+5 more)

### Community 6 - "Paneles Web (construcción) y QA"
Cohesion: 0.17
Nodes (12): Bandeja de validación de estadías (construcción), Etapa 17 documento, Panel admin de balneario (construcción), Panel operativo (construcción), Preview en vivo del theme (mismos tokens que mobile), Vinculación cuenta MP (UI panel admin), Etapa 19 documento, Pago vía Mercado Pago (sandbox) - testing (+4 more)

### Community 7 - "Etapas de Especificación y Diseño"
Cohesion: 0.27
Nodes (10): Etapa 01 — Visión, alcance MVP y glosario, Etapa 02 — Arquitectura general y multitenant, Etapa 03 — Modelo de datos (MySQL), Etapa 04 — Convenciones y contratos de API REST, Etapa 05 — Seguridad, autenticación, roles y permisos, Etapa 06 — Identidad visual y sistema de diseño, Etapa 07 — Diseño UX/UI app mobile, Etapa 08 — Diseño UX/UI web paneles (+2 more)

### Community 8 - "Etapas de Construcción Backend/Deploy"
Cohesion: 0.22
Nodes (10): Etapa 09 — Backend fundacional, Etapa 10 — Backend Super Admin, Etapa 11 — Backend catálogo y ubicaciones, Etapa 12 — Backend estadía activa, Etapa 13 — Backend carrito, pedidos, tiempo real, Etapa 14 — Backend servicios al carpero y promociones, Etapa 15 — Backend reportes básicos, Etapa 18 — Web panel Super Admin (+2 more)

### Community 9 - "UX Mobile y Design System"
Cohesion: 0.22
Nodes (9): Mercado Pago Checkout API, Design system base (componentes), Etapa 06 documento, Identidad de plataforma EasyBeach, Test white-label (criterio de aceptación), Carrito, pago y confirmación (Mercado Pago in-app), Etapa 07 documento, Home de estadía activa (+1 more)

### Community 10 - "Diseño UX de Paneles"
Cohesion: 0.22
Nodes (9): Bandeja de validación de estadías (UI), Configuración visual con preview en vivo, Estado de vinculación con Mercado Pago (UI), Etapa 08 documento, Panel admin de balneario, Panel operativo (barra/cocina/carpero), Panel Super Admin, Endpoint resumen de KPIs para dashboard (+1 more)

## Ambiguous Edges - Review These
- `graphify` → `Plan de ejecución por etapas`  [AMBIGUOUS]
  CLAUDE.md · relation: conceptually_related_to

## Knowledge Gaps
- **53 isolated node(s):** `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)`, `Etapa 15 — Backend reportes básicos`, `Endpoint resumen de KPIs para dashboard` (+48 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **5 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `graphify` and `Plan de ejecución por etapas`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Validación de apertura de estadía por el carpero` connect `Estadía Activa y Dominio Operativo` to `Visión, Stack y Objetivo de Negocio`, `Diseño UX de Paneles`, `API, Seguridad y Multitenancy`?**
  _High betweenness centrality (0.164) - this node is a cross-community bridge._
- **Why does `Etapa 01 documento` connect `Visión, Stack y Objetivo de Negocio` to `Estadía Activa y Dominio Operativo`, `Pagos Mercado Pago Marketplace`, `Theming White-Label por Balneario`?**
  _High betweenness centrality (0.141) - this node is a cross-community bridge._
- **Why does `Etapa 03 documento` connect `Modelo de Datos y Pedidos` to `Estadía Activa y Dominio Operativo`, `Pagos Mercado Pago Marketplace`, `API, Seguridad y Multitenancy`?**
  _High betweenness centrality (0.121) - this node is a cross-community bridge._
- **What connects `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)` to the rest of the system?**
  _55 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Estadía Activa y Dominio Operativo` be split into smaller, more focused modules?**
  _Cohesion score 0.10276679841897234 - nodes in this community are weakly interconnected._
- **Should `Modelo de Datos y Pedidos` be split into smaller, more focused modules?**
  _Cohesion score 0.1323529411764706 - nodes in this community are weakly interconnected._