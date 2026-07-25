# Graph Report - .  (2026-07-24)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 641 nodes · 1193 edges · 57 communities (42 shown, 15 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 111 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5f0f6aac`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Usuario|Usuario]]
- [[_COMMUNITY_AuthService.java|AuthService.java]]
- [[_COMMUNITY_JwtProperties|JwtProperties]]
- [[_COMMUNITY_Especificación Etapa 09 — Backend Fundacional (retrospectiva de implementación)|Especificación Etapa 09 — Backend Fundacional (retrospectiva de implementación)]]
- [[_COMMUNITY_UsuarioBalnearioRol|UsuarioBalnearioRol]]
- [[_COMMUNITY_AuthFlowIntegrationTest|AuthFlowIntegrationTest]]
- [[_COMMUNITY_.build|.build]]
- [[_COMMUNITY_Módulo backend platform|Módulo backend: platform]]
- [[_COMMUNITY_Etapa 07 — Diseño UXUI app mobile (cliente)|Etapa 07 — Diseño UX/UI app mobile (cliente)]]
- [[_COMMUNITY_Entregable docsdesigneasybeach-panel-operativo|Entregable: docs/design/easybeach-panel-operativo/]]
- [[_COMMUNITY_Etapa 02 — Arquitectura general y estrategia multitenant (documento)|Etapa 02 — Arquitectura general y estrategia multitenant (documento)]]
- [[_COMMUNITY_ADR-005 Theming white-label runtime servido por API|ADR-005: Theming white-label runtime servido por API]]
- [[_COMMUNITY_Módulo backend stay|Módulo backend: stay]]
- [[_COMMUNITY_SecurityConfig.java|SecurityConfig.java]]
- [[_COMMUNITY_Módulo backend ordering|Módulo backend: ordering]]
- [[_COMMUNITY_Etapa 13 documento|Etapa 13 documento]]
- [[_COMMUNITY_Etapa 16 documento|Etapa 16 documento]]
- [[_COMMUNITY_AbstractIntegrationTest|AbstractIntegrationTest]]
- [[_COMMUNITY_.generate|.generate]]
- [[_COMMUNITY_RequestIdFilter.java|RequestIdFilter.java]]
- [[_COMMUNITY_Etapa 03 — Modelo de datos MySQL (documento)|Etapa 03 — Modelo de datos MySQL (documento)]]
- [[_COMMUNITY_Tabla pedido|Tabla pedido]]
- [[_COMMUNITY_Etapa 10 documento|Etapa 10 documento]]
- [[_COMMUNITY_Mercado Pago Marketplace (application_fee=0)|Mercado Pago Marketplace (application_fee=0)]]
- [[_COMMUNITY_Etapa 12 documento|Etapa 12 documento]]
- [[_COMMUNITY_Etapa 05 — Seguridad, autenticación, roles y permisos (documento)|Etapa 05 — Seguridad, autenticación, roles y permisos (documento)]]
- [[_COMMUNITY_Etapa 04 — Convenciones y contratos de API REST (documento)|Etapa 04 — Convenciones y contratos de API REST (documento)]]
- [[_COMMUNITY_Validación de apertura de estadía por el carpero|Validación de apertura de estadía por el carpero]]
- [[_COMMUNITY_Vinculación de Mercado Pago por balneario (OAuth) - backend|Vinculación de Mercado Pago por balneario (OAuth) - backend]]
- [[_COMMUNITY_JpaAuditingConfig.java|JpaAuditingConfig.java]]
- [[_COMMUNITY_JacksonMoneyConfig.java|JacksonMoneyConfig.java]]
- [[_COMMUNITY_CorsConfig.java|CorsConfig.java]]
- [[_COMMUNITY_ModuleDependencyRulesTest.java|ModuleDependencyRulesTest.java]]
- [[_COMMUNITY_EasyBeachApplication|EasyBeachApplication]]
- [[_COMMUNITY_Alcance MVP explícito|Alcance MVP explícito]]
- [[_COMMUNITY_Actor Super Admin|Actor: Super Admin]]
- [[_COMMUNITY_Etapa 14 — Backend servicios al carpero y promociones|Etapa 14 — Backend servicios al carpero y promociones]]
- [[_COMMUNITY_Graphify Knowledge Graph Workflow|Graphify Knowledge Graph Workflow]]
- [[_COMMUNITY_Endpoint group Operativo|Endpoint group: Operativo]]
- [[_COMMUNITY_Tabla auditoria_plataforma|Tabla auditoria_plataforma]]
- [[_COMMUNITY_Tabla estadia_ubicacion_historial|Tabla estadia_ubicacion_historial]]
- [[_COMMUNITY_Tabla pedido_promocion|Tabla pedido_promocion]]
- [[_COMMUNITY_Tabla promocion_alcance|Tabla promocion_alcance]]
- [[_COMMUNITY_Tabla promocion_combo_item|Tabla promocion_combo_item]]
- [[_COMMUNITY_Tabla usuario_balneario_rol|Tabla usuario_balneario_rol]]
- [[_COMMUNITY_Promociones básicas (descuento %, combo, happy hour)|Promociones básicas (descuento %, combo, happy hour)]]
- [[_COMMUNITY_Estacionalidad de la infraestructura|Estacionalidad de la infraestructura]]
- [[_COMMUNITY_Etapa 20 — Infraestructura, deploy y observabilidad|Etapa 20 — Infraestructura, deploy y observabilidad]]
- [[_COMMUNITY_com.easybeacheasybeach-backend|com.easybeach:easybeach-backend]]

## God Nodes (most connected - your core abstractions)
1. `Especificación Etapa 09 — Backend Fundacional (retrospectiva de implementación)` - 32 edges
2. `Backend README (EasyBeach)` - 25 edges
3. `Usuario` - 23 edges
4. `JwtProperties` - 23 edges
5. `AuthService` - 21 edges
6. `UsuarioBalnearioRol` - 20 edges
7. `UsuarioRepository` - 18 edges
8. `JwtService` - 17 edges
9. `Balneario` - 17 edges
10. `SesionRefresh` - 16 edges

## Surprising Connections (you probably didn't know these)
- `Módulo platform (balneario, el tenant)` --conceptually_related_to--> `TenantContext (ThreadLocal)`  [INFERRED]
  backend/README.md → docs/especificacion/09-backend-fundacional.md
- `Módulo identity (usuarios, roles, autenticación JWT, autorización)` --conceptually_related_to--> `Autenticación JWT RS256 (access + refresh)`  [INFERRED]
  backend/README.md → docs/especificacion/09-backend-fundacional.md
- `Backend README (EasyBeach)` --conceptually_related_to--> `ADR-001: convención de multitenancy`  [INFERRED]
  backend/README.md → docs/especificacion/09-backend-fundacional.md
- `Backend README (EasyBeach)` --conceptually_related_to--> `Autorización por rol (@PreAuthorize + verificación en service)`  [INFERRED]
  backend/README.md → docs/especificacion/09-backend-fundacional.md
- `Decisión: Maven sin wrapper (no Gradle)` --rationale_for--> `Backend README (EasyBeach)`  [EXTRACTED]
  docs/especificacion/09-backend-fundacional.md → backend/README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Aislamiento multitenant en tres capas** — docs_adr_adr_001_multitenancy_base_compartida_multitenancy, docs_adr_adr_001_multitenancy_base_compartida_tenantcontext, docs_adr_adr_001_multitenancy_base_compartida_hibernate_filter, docs_adr_adr_001_multitenancy_base_compartida_archunit_tenant_test [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace** — docs_adr_adr_004_pagos_mercadopago_marketplace_checkout_api, docs_especificacion_03_modelo_de_datos_tabla_pedido_pago, docs_especificacion_03_modelo_de_datos_tabla_balneario_mp_credencial, docs_api_openapi_pagos [INFERRED 0.85]
- **Sistema de theming white-label** — docs_adr_adr_005_theming_runtime_theming_runtime, docs_especificacion_03_modelo_de_datos_tabla_configuracion_visual, docs_api_openapi_publico, docs_etapas_06_identidad_visual_design_system_contrato_tokens_v1 [INFERRED 0.85]
- **Flujo de pago Mercado Pago marketplace de punta a punta** — docs_especificacion_01_vision_alcance_glosario_mercado_pago_marketplace, docs_etapas_10_backend_super_admin_vinculacion_mp_oauth, docs_etapas_13_backend_pedidos_webhook_mp_recepcion, docs_etapas_05_seguridad_roles_seguridad_pagos_mp, docs_etapas_03_modelo_de_datos_pedido_pago_tabla [INFERRED 0.90]
- **Flujo de validación de estadía por el carpero** — docs_especificacion_01_vision_alcance_glosario_validacion_estadia_carpero, docs_etapas_12_backend_estadia_activa_apertura_dos_pasos, docs_etapas_07_ux_ui_mobile_pantalla_espera_confirmacion, docs_etapas_08_ux_ui_web_paneles_bandeja_validacion_estadias, docs_etapas_04_contratos_api_bandeja_validacion_estadias [INFERRED 0.90]

## Communities (57 total, 15 thin omitted)

### Community 0 - "Usuario"
Cohesion: 0.06
Nodes (32): AfterEach, EstadoUsuario, Entity, Getter, NoArgsConstructor, Setter, Table, Usuario (+24 more)

### Community 1 - "AuthService.java"
Cohesion: 0.09
Nodes (26): EstadoSesion, Entity, Getter, NoArgsConstructor, Setter, Table, SesionRefresh, SesionRefreshRepository (+18 more)

### Community 2 - "JwtProperties"
Cohesion: 0.06
Nodes (19): Bean, Configuration, Logger, JwtKeyPair, JwtKeyProvider, JwtProperties, AccessToken, SecureRandom (+11 more)

### Community 3 - "Especificación Etapa 09 — Backend Fundacional (retrospectiva de implementación)"
Cohesion: 0.09
Nodes (45): Backend README (EasyBeach), Módulo branding [etapa 10] (configuración visual white-label), Módulo catalog [etapa 11] (menú, productos, variantes), Módulo concierge [etapa 14] (servicios al carpero), Módulo identity (usuarios, roles, autenticación JWT, autorización), Módulo ordering [etapa 13] (carrito, pedidos, colas operativas), Módulo payments [etapa 13] (Mercado Pago: OAuth, webhook), Módulo platform (balneario, el tenant) (+37 more)

### Community 4 - "UsuarioBalnearioRol"
Cohesion: 0.09
Nodes (25): Entity, Getter, NoArgsConstructor, Setter, Table, UsuarioBalnearioRol, UsuarioBalnearioRolRepository, Service (+17 more)

### Community 5 - "AuthFlowIntegrationTest"
Cohesion: 0.12
Nodes (15): Entity, Getter, NoArgsConstructor, Setter, Table, Rol, RolCodigo, TipoUsuario (+7 more)

### Community 6 - ".build"
Cohesion: 0.18
Nodes (16): AccessDeniedException, AuthenticationException, defaultDetail(), ErrorCode, status(), FieldErrorDetail, GlobalExceptionHandler, HttpServletRequest (+8 more)

### Community 7 - "Módulo backend: platform"
Cohesion: 0.20
Nodes (12): Endpoint group: Auth, Módulo backend: branding, Módulo backend: identity, Módulo backend: platform, Módulo backend: reporting, Módulo backend: shared, Tabla plan, Tabla rol (+4 more)

### Community 8 - "Etapa 07 — Diseño UX/UI app mobile (cliente)"
Cohesion: 0.11
Nodes (18): Apertura de estadía validada por carpero, App mobile cliente (React Native), Carrito, pago Mercado Pago y confirmación, Cierre de estadía (resumen de consumo), Uso de tokens del design system (etapa 06), Entregable Etapa 07: prototipo de 32 pantallas (s01–s32), Estados no felices (sin conexión, cerrado, no disponible), Etapa 07 — Diseño UX/UI app mobile (cliente) (+10 more)

### Community 9 - "Entregable: docs/design/easybeach-panel-operativo/"
Cohesion: 0.16
Nodes (18): Etapa 08 — Diseño UX/UI web (paneles), ABM de menú (categorías, variantes, fotos, disponibilidad), ubicaciones, promociones, staff, Bandeja de validación de estadías, Cola de pedidos entrantes en vivo, Cola de solicitudes de servicio al carpero, Configuración visual white-label con preview en vivo, Dashboard: facturación, pedidos, ticket promedio, productos más vendidos, Entregable: docs/design/easybeach-panel-operativo/ (+10 more)

### Community 10 - "Etapa 02 — Arquitectura general y estrategia multitenant (documento)"
Cohesion: 0.13
Nodes (17): Opción: microservicios (descartada), ADR-002: Monolito modular por capas, Reglas de dependencia entre módulos (ArchUnit), Reconexión Last-Event-ID + heartbeat, Opción: polling puro (descartada como único canal), ADR-003: Tiempo real — SSE con fallback polling, SseEmitter de Spring, Opción: WebSocket/STOMP (descartada) (+9 more)

### Community 11 - "ADR-005: Theming white-label runtime servido por API"
Cohesion: 0.15
Nodes (16): Opción: builds white-label por balneario (descartada), Contrato de tokens de theming (versionado), Endpoint público GET /balnearios/{id}/branding, ThemeProvider raíz (runtime), ADR-005: Theming white-label runtime servido por API, Endpoint group: Público, Claude Design handoff bundle README (Etapa 06), EasyBeach Etapa 06.dc.html (prototipo primario) (+8 more)

### Community 12 - "Módulo backend: stay"
Cohesion: 0.29
Nodes (8): Eventos de dominio in-process (ApplicationEventPublisher), Endpoint group: Servicios, Eventos de dominio (contrato interno), Módulo backend: concierge, Módulo backend: stay, Tabla solicitud_servicio, Tabla tipo_servicio, Tabla ubicacion

### Community 13 - "SecurityConfig.java"
Cohesion: 0.33
Nodes (8): Bean, Configuration, PasswordEncoder, SecurityConfig, EnableMethodSecurity, EnableWebSecurity, HttpSecurity, SecurityFilterChain

### Community 14 - "Módulo backend: ordering"
Cohesion: 0.24
Nodes (10): Endpoint group: Admin balneario, Módulo backend: catalog, Módulo backend: ordering, Módulo backend: promotions, Tabla categoria_menu, Tabla pedido_evento, Tabla pedido_item, Tabla producto (+2 more)

### Community 15 - "Etapa 13 documento"
Cohesion: 0.20
Nodes (11): Aplicación de promociones en el pedido, Colas operativas por balneario, Etapa 13 documento, Test de idempotencia de pedido, Máquina de estados completa del pedido (etapa 13), Mercado Pago Marketplace (pago del pedido), Precios e ítems congelados (histórico inmutable sobre variante), Tiempo real: notificación de cambio de estado (+3 more)

### Community 16 - "Etapa 16 documento"
Cohesion: 0.18
Nodes (11): Base técnica React Native, Etapa 16 documento, Notificaciones push, Tolerancia a conectividad de playa (offline), Etapa 19 documento, Pago vía Mercado Pago (sandbox) - testing, Prueba de carga básica (sábado de enero), Seeds y datos demo (balnearios ficticios) (+3 more)

### Community 17 - "AbstractIntegrationTest"
Cohesion: 0.33
Nodes (7): ActiveProfiles, EasyBeachApplicationTests, Test, AbstractIntegrationTest, MySQLContainer, SpringBootTest, TestRestTemplate

### Community 18 - ".generate"
Cohesion: 0.29
Nodes (4): SecureRandom, UlidGenerator, Test, UlidGeneratorTest

### Community 19 - "RequestIdFilter.java"
Cohesion: 0.33
Nodes (8): Component, FilterChain, HttpServletRequest, HttpServletResponse, Override, RequestIdFilter, OncePerRequestFilter, Order

### Community 20 - "Etapa 03 — Modelo de datos MySQL (documento)"
Cohesion: 0.16
Nodes (15): Regla ArchUnit @TenantScoped + batería cross-tenant, Discriminador balneario_id, Opción: base por tenant (descartada), Hibernate @Filter / @TenantScoped, ADR-001: Multitenancy — base compartida con balneario_id, Opción: schema por tenant (descartada), TenantContext request-scoped, Convenciones normativas del modelo de datos (+7 more)

### Community 21 - "Tabla pedido"
Cohesion: 0.18
Nodes (11): Idempotencia doble (pedido + webhook), Máquina de estados pago↔pedido, Endpoint group: Estadía, Endpoint group: Pedidos, Máquina de estados: pago, Máquina de estados: pedido, Tabla estadia, Tabla mp_webhook_notificacion (+3 more)

### Community 22 - "Etapa 10 documento"
Cohesion: 0.20
Nodes (10): Actor: Admin de balneario, Balneario (tenant), Theming White-Label Total, ABM de balnearios, Endpoints de configuración visual (theme completo), Endpoint público de branding, Etapa 10 documento, Listado público de balnearios activos (+2 more)

### Community 23 - "Mercado Pago Marketplace (application_fee=0)"
Cohesion: 0.22
Nodes (10): Mercado Pago Checkout API, EasyBeach, Mercado Pago Marketplace (application_fee=0), Objetivo rector (vender más), Actor: Operador de barra/cocina, Pago, Pedido, Producto con variantes (+2 more)

### Community 24 - "Etapa 12 documento"
Cohesion: 0.20
Nodes (10): Categorías de menú (ABM), Etapa 11 documento, Menú público del balneario (endpoint), Productos y variantes (ABM), Ubicaciones (ABM), Cambio de ubicación dentro del mismo balneario, Estado CERRADA_POR_SISTEMA (fin de temporada), Cierre explícito de estadía con resumen de consumo (+2 more)

### Community 25 - "Etapa 05 — Seguridad, autenticación, roles y permisos (documento)"
Cohesion: 0.18
Nodes (15): application_fee = 0 constante del servidor, ADR-004: Mercado Pago Checkout API — marketplace sin comisión, OAuth vinculación MP del balneario, Job de reconciliación de pagos pendientes, Webhook payment.updated + verificación server-to-server, Endpoint group: Pagos, Módulo backend: payments, Tabla balneario_mp_credencial (+7 more)

### Community 26 - "Etapa 04 — Convenciones y contratos de API REST (documento)"
Cohesion: 0.25
Nodes (9): EasyBeach API — OpenAPI 3.0.3 spec, Endpoint group: SuperAdmin, Tabla balneario, Convenciones generales de API (versionado, códigos, RFC 7807), Etapa 04 — Convenciones y contratos de API REST (documento), Matriz endpoint → rol, Matriz pantalla → endpoints, Matriz de roles y permisos (cerrada) (+1 more)

### Community 27 - "Validación de apertura de estadía por el carpero"
Cohesion: 0.25
Nodes (9): Carpero, Actor: Cliente, Estadía activa, Servicio al carpero, Ubicación, Unicidad de estadía: una por cliente+balneario, Validación de apertura de estadía por el carpero, Apertura de estadía en dos pasos (solicitud + validación carpero) (+1 more)

### Community 28 - "Vinculación de Mercado Pago por balneario (OAuth) - backend"
Cohesion: 0.25
Nodes (8): Vinculación OAuth cuenta MP del balneario, Vinculación de Mercado Pago por balneario (OAuth) - backend, Bandeja de validación de estadías (construcción), Etapa 17 documento, Panel admin de balneario (construcción), Panel operativo (construcción), Preview en vivo del theme (mismos tokens que mobile), Vinculación cuenta MP (UI panel admin)

### Community 29 - "JpaAuditingConfig.java"
Cohesion: 0.43
Nodes (5): Bean, Configuration, JpaAuditingConfig, DateTimeProvider, EnableJpaAuditing

### Community 30 - "JacksonMoneyConfig.java"
Cohesion: 0.53
Nodes (4): JacksonMoneyConfig, Bean, Configuration, Jackson2ObjectMapperBuilderCustomizer

### Community 31 - "CorsConfig.java"
Cohesion: 0.53
Nodes (4): CorsConfig, Bean, Configuration, CorsFilter

### Community 32 - "ModuleDependencyRulesTest.java"
Cohesion: 0.53
Nodes (3): Test, ModuleDependencyRulesTest, JavaClasses

### Community 33 - "EasyBeachApplication"
Cohesion: 0.60
Nodes (3): EasyBeachApplication, ConfigurationPropertiesScan, SpringBootApplication

## Knowledge Gaps
- **126 isolated node(s):** `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)`, `Etapa 15 — Backend reportes básicos`, `Endpoint resumen de KPIs para dashboard` (+121 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `JwtService` connect `JwtProperties` to `Usuario`, `AuthService.java`, `SecurityConfig.java`?**
  _High betweenness centrality (0.055) - this node is a cross-community bridge._
- **Why does `UsuarioRepository` connect `Usuario` to `AuthService.java`, `AuthFlowIntegrationTest`, `SecurityConfig.java`?**
  _High betweenness centrality (0.033) - this node is a cross-community bridge._
- **Why does `Usuario` connect `Usuario` to `AuthService.java`, `UsuarioBalnearioRol`, `AuthFlowIntegrationTest`?**
  _High betweenness centrality (0.027) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `Backend README (EasyBeach)` (e.g. with `ADR-001: convención de multitenancy` and `Autorización por rol (@PreAuthorize + verificación en service)`) actually correct?**
  _`Backend README (EasyBeach)` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `Usuario` (e.g. with `.seedStaffUser()` and `.crearMiembro()`) actually correct?**
  _`Usuario` has 2 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Etapa 14 — Backend servicios al carpero y promociones`, `Solicitud de servicio al carpero (implementación)`, `Promociones básicas (descuento %, combo, happy hour)` to the rest of the system?**
  _133 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Usuario` be split into smaller, more focused modules?**
  _Cohesion score 0.056189640035118525 - nodes in this community are weakly interconnected._