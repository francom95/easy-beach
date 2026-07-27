# EasyBeach — Plan de ejecución por etapas

Plataforma SaaS multibalneario para balnearios de playa (Argentina).
**Objetivo rector:** que los balnearios vendan más. Toda decisión de producto y
arquitectura se evalúa contra ese objetivo: facilitar el consumo del cliente
durante su estadía (pedir sin moverse, promociones, combos, recompra) y aumentar
la facturación operativa en temporada.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java Spring Boot + MySQL (API REST, modular, por capas, multitenant) |
| Mobile (cliente) | React Native (Android / iOS) |
| Web (paneles) | React / Next.js |

> **Nota de identidad visual (decisión de producto):** el theming por
> balneario es **white-label total** — al seleccionar un balneario, toda la
> estética de la app cliente se transforma como si la app fuera del
> balneario; la marca EasyBeach solo aparece antes de la selección y en los
> paneles de staff. Los pagos son in-app vía **Mercado Pago Checkout API**,
> modelo marketplace sin comisión de plataforma (cada balneario cobra en su
> propia cuenta). Detalle y demás decisiones cerradas:
> `docs/especificacion/01-vision-alcance-glosario.md`.

## Cómo leer este plan

- Cada etapa tiene un archivo propio `NN-nombre.md` con: **modelo ejecutor**,
  objetivo, alcance, dependencias, inputs requeridos (si aplica) y criterios de
  aceptación.
- **Modelo ejecutor**: qué modelo conviene usar para ejecutar la etapa
  (`fable` > `opus` > `sonnet` > `haiku` en capacidad/costo), o
  **`claude design`** cuando es diseño visual que se hace a mano.
- Las etapas 01–05 son de **especificación** (documentos, sin código).
  Las 06–08 son de **diseño visual**. Las 09–20 son de **construcción**.
- Las etapas de diseño (06–08) pueden correr en paralelo con las de backend
  (09–15): el backend no depende del diseño visual.

## Índice de etapas

| # | Etapa | Modelo | Depende de |
|---|---|---|---|
| 01 | ✅ [Visión, alcance MVP y glosario de dominio](01-vision-alcance-glosario.md) | sonnet | — |
| 02 | ✅ [Arquitectura general y estrategia multitenant](02-arquitectura-multitenant.md) | fable | 01 |
| 03 | ✅ [Modelo de datos (MySQL)](03-modelo-de-datos.md) | opus | 01, 02 |
| 04 | ✅ [Convenciones y contratos de API REST](04-contratos-api.md) | opus | 02, 03 |
| 05 | ✅ [Seguridad, autenticación, roles y permisos](05-seguridad-roles.md) | opus | 02, 03 |
| 06 | ✅ [Identidad visual y sistema de diseño](06-identidad-visual-design-system.md) | claude design | 01 |
| 07 | ✅ [Diseño UX/UI app mobile (cliente)](07-ux-ui-mobile.md) | claude design | 01, 06 |
| 08 | ✅ [Diseño UX/UI web (paneles)](08-ux-ui-web-paneles.md) | claude design | 01, 06 |
| 09 | ✅ [Backend fundacional](09-backend-fundacional.md) | sonnet | 02, 03, 04, 05 |
| 10 | ✅ [Backend Super Admin: balnearios, planes y temporadas](10-backend-super-admin.md) | sonnet | 09 |
| 11 | ✅ [Backend catálogo y ubicaciones](11-backend-catalogo-ubicaciones.md) | sonnet | 09, 10 |
| 12 | ✅ [Backend estadía activa](12-backend-estadia-activa.md) | opus | 09, 10, 11 |
| 13 | ✅ [Backend carrito, pedidos, estados y tiempo real](13-backend-pedidos.md) | opus | 11, 12 |
| 14 | ✅ [Backend servicios al carpero y promociones](14-backend-carpero-promociones.md) | sonnet | 12, 13 |
| 15 | ✅ [Backend reportes básicos](15-backend-reportes.md) | haiku | 13, 14 |
| 16 | ✅ [Mobile app cliente](16-mobile-app-cliente.md) | sonnet | 07, 09–14 |
| 17 | [Web: panel operativo y panel admin de balneario](17-web-panel-operativo-admin.md) | sonnet | 08, 10–15 |
| 18 | [Web: panel Super Admin](18-web-panel-super-admin.md) | haiku | 08, 10 |
| 19 | [QA integral, seeds y datos demo](19-qa-seeds-demo.md) | sonnet | 09–18 |
| 20 | [Infraestructura, deploy y observabilidad](20-infra-deploy.md) | sonnet | 09; cierre tras 19 |

## Hitos

1. **Hito Especificación** (fin etapa 05): arquitectura, datos, API y seguridad
   definidos. Se puede empezar a construir.
2. ✅ **Hito Backend MVP** (fin etapa 15): API completa operable por Postman/tests.
3. **Hito Producto MVP** (fin etapa 18): cliente pide desde la app, el balneario
   opera desde el panel.
4. **Hito Go-live** (fin etapa 20): desplegado, monitoreado, con datos demo para
   vender la plataforma a balnearios.
