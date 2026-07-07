# ADR-005 — Theming white-label: una sola app con theme runtime servido por API

- **Estado:** Aceptada (decisión de producto cerrada en etapa 01)
- **Fecha:** 2026-07-07
- **Etapa:** 02

## Contexto

Decisión de producto (etapa 01): al seleccionar un balneario, **toda la
estética de la app cliente se transforma como si la app fuera del balneario**
(white-label total). Hay que decidir cómo se implementa esa transformación.

## Opciones evaluadas

**(a) Builds white-label por balneario.** Una app por balneario en las stores,
compilada con su marca.
- ✅ White-label absoluto, incluso el ícono y la ficha de store.
- ❌ N pipelines de build, N fichas de store, N reviews de Apple por cada
  cambio; el alta de un balneario pasa de minutos a semanas. Mata el modelo
  SaaS de alta por temporada. Además el cliente que visita dos balnearios
  necesitaría dos apps.

**(b) Una sola app, theme resuelto en runtime desde la API.**
- ✅ Alta de balneario instantánea (es un registro + assets); un cliente, N
  balnearios en la misma app; un solo pipeline de release.
- ❌ La ficha de store y el ícono son de EasyBeach (límite aceptado y
  explicitado en etapa 01: la marca EasyBeach vive fuera de la estadía).
- ❌ El theme llega por red → hay que resolver carga y cache.

## Decisión

**Opción (b): una sola app React Native con theming runtime total.**

Mecanismo (normativo):
1. **Contrato de tokens** (definido en etapa 06): paleta completa, tipografía
   de un set curado, logo, portada, splash de entrada, radios/acentos. El
   contrato incluye un campo `version`.
2. **Endpoint público de branding** (etapa 10):
   `GET /api/v1/balnearios/{id}/branding` devuelve el theme completo, con
   `ETag`/`version` para cache. Público: se consume antes del login.
3. **Aplicación en runtime** (etapa 16): un `ThemeProvider` raíz re-renderiza
   la app al cambiar de balneario; **ningún componente usa colores/tipografías
   hardcodeadas** — solo tokens (regla verificable por lint).
4. **Cache y offline:** último theme persistido localmente; al reabrir la app
   en la playa se aplica el theme cacheado al instante y se revalida por ETag
   en background. Fallback al theme neutro EasyBeach únicamente fuera de una
   selección de balneario.
5. **Tipografías:** el set curado se **embebe en el binario** (no se descargan
   fuentes arbitrarias): evita fallas de carga, problemas de licencias y
   garantiza legibilidad; el token elige entre las embebidas.
6. **Assets pesados** (logo, portada, splash) por CDN/URL pública con cache
   local; el splash de transición usa el asset cacheado si existe, o una
   transición neutra la primera vez.

## Consecuencias

- El "test white-label" de la etapa 06 (ninguna marca EasyBeach dentro de la
  estadía) es alcanzable en una sola app; el único límite es la ficha de store.
- El contrato de tokens es un **contrato de tres consumidores** (API, mobile,
  web-preview del panel admin): versionarlo desde el día uno evita que un
  token nuevo rompa apps viejas (tokens desconocidos se ignoran; tokens
  faltantes toman default del design system).
- Los paneles web de staff **no** son white-label (marca EasyBeach): solo el
  preview del panel admin renderiza el theme del balneario, con los mismos
  tokens que mobile.
