# Contrato de tokens de theming — v1.0

- **Fuente:** entregable de la Etapa 06 (Claude Design) —
  [`easybeach-sistema-de-theming-white-label-1-identidad-de-plat/`](easybeach-sistema-de-theming-white-label-1-identidad-de-plat/)
  (prototipo HTML con identidad, design system y mockups de validación).
- **Consumidores:** etapa 10 (API de branding — sirve exactamente este payload),
  etapa 16 (mobile runtime), etapa 17 (preview en panel admin web).
- **Regla de versionado:** cualquier token nuevo exige subir `theme.contract`.
  Tokens desconocidos se ignoran; faltantes toman el default (theme EasyBeach).
- **Principio rector:** el balneario es dueño de la estética; la plataforma es
  dueña de la estructura. Todo lo que se ve tiene token; lo que no tiene token
  es visualmente neutro (gris estructural, sin rasgos de marca EasyBeach).

## Leyenda de "¿Personalizable?"

- **Sí** — lo elige el balneario libremente.
- **Sí (validado)** — lo elige el balneario; el configurador (etapa 10, con
  preview en vivo) valida contraste y, si falla, propone el tono más cercano
  que cumple. La API nunca sirve un theme inválido.
- **Derivado** — lo calcula la plataforma a partir de otro token (nunca lo
  elige el balneario): para cada `on-*` se toma blanco o tinta oscura según
  mayor contraste; muted/border por desplazamiento de luminosidad en OKLCH
  manteniendo el matiz.
- **No** — común a toda la plataforma.

## Tokens

| Token | Tipo | Default (theme EasyBeach) | ¿Personalizable? |
|---|---|---|---|
| `theme.contract` | string (semver) | `"1.0"` | No — metadato de versión |
| `theme.name` | string | `"EasyBeach"` | Sí |
| **Colores** | | | |
| `color.primary` | color (hex) | `#C95100` | Sí (validado) |
| `color.on-primary` | color (hex) | `#FFFFFF` | Derivado de primary |
| `color.secondary` | color (hex) | `#17437B` | Sí (validado) |
| `color.on-secondary` | color (hex) | `#FFFFFF` | Derivado de secondary |
| `color.background` | color (hex) | `#F5EFE2` | Sí (validado) |
| `color.on-background` | color (hex) | `#1B2B40` | Derivado de background |
| `color.surface` | color (hex) | `#FFFFFF` | Sí (validado) |
| `color.on-surface` | color (hex) | `#1B2B40` | Derivado de surface |
| `color.on-surface-muted` | color (hex) | `#4A5A72` | Derivado de surface |
| `color.border` | color (hex) | `#E8DEC9` | Derivado de surface + background |
| `color.success` | color (hex) | `#1E7D3C` | Sí (validado) |
| `color.warning` | color (hex) | `#B25E00` | Sí (validado) |
| `color.error` | color (hex) | `#C22F2F` | Sí (validado) |
| `color.info` | color (hex) | `#1D62B4` | Sí (validado) |
| `color.on-state` | color (hex) × 4 | `#FFFFFF` × 4 | Derivado de cada estado |
| **Tipografía** | | | |
| `typography.family` | enum (set curado) | `"clara"` | Sí — 1 de 4 parejas |
| `typography.scale` | objeto (px fijos) | display 30 · title 22 · body 17 · label 14 · price 20 | No — común (legibilidad al sol) |
| **Assets** | | | |
| `asset.logo` | imagen (SVG/PNG, fondo transparente, alto ≥ 96 px) | logo EasyBeach | Sí |
| `asset.logo-compact` | imagen cuadrada 1:1 ≥ 128 px | isologo EasyBeach | Sí |
| `asset.cover` | imagen 16:9 ≥ 1600 px | fotografía genérica de playa | Sí |
| `asset.splash` | imagen 9:19.5 ≥ 1170 px, o `color.primary` + `asset.logo` | splash EasyBeach | Sí |
| `asset.product-placeholder` | imagen 1:1 ≥ 600 px | patrón neutro derivado de `color.background` | Sí (opcional) |

## Set curado de tipografías (`typography.family`)

Cuatro parejas embebidas en el binario de la app (licencia OFL), auditadas para
cuerpo 17 px al sol. El balneario elige una; no puede subir fuentes.

| Valor | Pareja (display + UI) | Carácter |
|---|---|---|
| `"clara"` | Lexend | Neutra, máxima legibilidad — **default** |
| `"amigable"` | Baloo 2 + Nunito | Redondeada, familiar, luminosa |
| `"elegante"` | Marcellus + Figtree | Serif clásica display, sans limpia UI |
| `"energica"` | Archivo | Condensada y pesada en display, regular en UI |

## Fuera del contrato (comunes, viven en el binario, no en la API)

Escala de espaciado (base 4: 4/8/12/16/24/32), radios (10 control · 16 card ·
999 chip/CTA), sombras (2 niveles), tamaños tipográficos, iconografía funcional
(lineal, geométrica), duración de animaciones y layout de componentes.

## Reglas de accesibilidad (resumen normativo)

1. Los pares `on-*` **nunca se eligen**: se derivan. Es imposible configurar
   texto ilegible.
2. Validación al guardar: ≥ 4.5:1 texto normal, ≥ 3:1 texto grande y
   componentes UI. Si falla, se propone el tono más cercano que cumple; el
   guardado exige aceptarlo o corregir.
3. Piso AA; para playa: precios, CTA de pedido y estados de pedido apuntan a
   ≥ 7:1 (AAA) — el configurador lo marca como "recomendado playa". Estados
   siempre con ícono + texto, nunca solo color.

## Transición de marca (spec para etapa 16)

1. Tap en la card del balneario (última pantalla con marca EasyBeach).
2. Expansión radial de `color.primary` desde la card (250 ms).
3. Splash del balneario (`asset.splash` + logo) mientras carga el theme
   (mín. 600 ms, máx. 2 s con caché).
4. Home de estadía 100 % tematizado; desde ahí, cero marca EasyBeach.

Cache: el theme se cachea al confirmar la estadía; aperturas siguientes van
directo al splash del balneario. Sin red: splash + último theme cacheado —
jamás fallback con marca EasyBeach dentro de una estadía. Volver al selector
(cerrar estadía) es el único camino de regreso a la marca EasyBeach.
