# Etapa 11 — Backend catálogo y ubicaciones

- **Estado:** ejecutada. Insumo directo de la etapa 12 (estadía — necesita
  `Ubicacion`) y la etapa 13 (pedidos — necesita `Producto`/`ProductoVariante`
  para congelar precios).
- **Corresponde al plan:** [docs/etapas/11-backend-catalogo-ubicaciones.md](../etapas/11-backend-catalogo-ubicaciones.md)
- **Depende de:** [09](09-backend-fundacional.md), [10](10-backend-super-admin.md).
- **Código:** [`backend/`](../../backend/) — módulos `catalog` (completo) y
  `stay` (solo `Ubicacion`, adelantada del resto que construye la etapa 12).

## 1. Qué se construyó

### Catálogo (`catalog`)
ABM completo de `CategoriaMenu` → `Producto` → `ProductoVariante`, los tres
`@TenantScoped`. Reglas de integridad **reales** (no diferidas, a diferencia
de las de ubicación/estadía en la etapa 10):

- **No se borra una categoría con productos** — verificado con
  `ProductoRepository.existsByCategoriaId`, ya que `Producto` existe en esta
  misma etapa (a diferencia de "no suspender balneario con estadías
  abiertas" en la etapa 10, que quedó como hook porque `Estadia` no existía).
- **Disponibilidad on/off inmediata**: `PUT /admin/productos/{id}/disponibilidad`
  es un endpoint dedicado y liviano (no reescribe el resto del producto) —
  el caso de uso real es "se terminó el hielo", una acción de un tap.
- **Precio congelado por variante, no por producto** (etapa 03 §3.4): el
  precio autoritativo es el de la variante elegida si el producto tiene
  variantes; `producto.precioBase` solo aplica si no tiene ninguna.

### Menú público (`GET /api/v1/balnearios/{slug}/menu`)
El endpoint más consultado de la plataforma: categorías activas ordenadas →
productos disponibles de cada una → variantes disponibles de cada producto,
en **3 queries totales** (no N+1: variantes resueltas por lote con
`findByProductoIdInAndDisponibleTrue`, agrupadas en memoria). `Cache-Control:
max-age=15` + `ETag` (`ShallowEtagHeaderFilter`, registrado **solo** en esta
ruta vía `FilterRegistrationBean` — no globalmente, y no como `@Bean Filter`
directo por la misma razón documentada en la etapa 09: evitar el
auto-registro prematuro de Spring Boot). Promociones vigentes (etapa 14) se
embeben acá cuando ese módulo exista — deferred, documentado.

### Ubicaciones (`stay`, adelantado)
ABM de `Ubicacion` (tipo, identificador, estado), única por
`(balneario_id, identificador)` entre las no borradas. La restricción
parcial (`UNIQUE` que ignore borrados) no existe nativamente en MySQL — se
resuelve con una **columna generada** (`identificador_uk`, `STORED`, colapsa
a `NULL` si `deleted_at` no es nulo) sobre la que sí se puede declarar un
`UNIQUE` normal, ya que MySQL trata múltiples `NULL` como distintos (mismo
truco que la unicidad de estadía activa en la etapa 03).

"No desactivar/borrar una ubicación con estadía activa" queda como
`// TODO(etapa 12)` explícito en `UbicacionService` — la entidad `Estadia`
no existe todavía.

## 2. Sobre la marcha: corregido un status code incorrecto (no un bug nuevo)

Al escribir las validaciones de esta etapa noté que la etapa 04 especifica
`422` para "datos inválidos" (§1.2), pero `ErrorCode.VALIDACION_FALLIDA`
(etapa 09) usaba `400`. **Corregido** a `422`. También separé un código
nuevo, `CONFLICTO_DE_ESTADO` (`409`), para violaciones de unicidad/estado
(categoría con productos, ubicación duplicada) — antes se hubiera mezclado
con `VALIDACION_FALLIDA`, que la spec reserva para datos mal formados, no
conflictos de estado.

**Alcance de la corrección:** el default del código y el código nuevo de
esta etapa en adelante están bien. **No** audité retroactivamente cada uso
de `VALIDACION_FALLIDA` en las etapas 09/10 (ej. "ya existe un balneario con
ese slug", "transición inválida" de temporada/suscripción) - varios de esos
son semánticamente `409`, no `422`, y quedaron sin tocar. Señalado acá en
vez de silenciado; una limpieza retroactiva es trabajo legítimo pero
separado, no de esta etapa.

## 3. Cómo se verificó

**25/25 tests, BUILD SUCCESS**, contra MySQL 8 real — **sin bugs nuevos
encontrados** en esta etapa (a diferencia de las etapas 09/10):

- `CatalogoMenuIntegrationTest` — flujo completo: categoría → producto con
  2 variantes → el menú público las devuelve todas en una sola llamada →
  apagar disponibilidad lo saca del menú público al instante → borrar
  categoría con productos falla (409) → borrar el producto primero, ENTONCES
  sí se puede borrar la categoría. Más: ubicación duplicada rechazada (409).
- `CatalogoCrossTenantIntegrationTest` — criterio de aceptación explícito:
  el admin del balneario B no puede editar/borrar la categoría ni la
  ubicación del balneario A (404, no 403 — "recurso ajeno" por ownership,
  etapa 05 §2 regla 1), y los listados de B nunca incluyen datos de A.
- Suite completa de las etapas 09/10 sigue en verde: sin regresiones.
