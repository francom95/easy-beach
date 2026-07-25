# Etapa 11 — Backend catálogo y ubicaciones

- **Orden:** 11
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 09, 10
- **Estado: ✅ EJECUTADA** — código en [`backend/`](../../backend/) (módulo
  `catalog` completo + `Ubicacion` del módulo `stay`), entregable en
  [`docs/especificacion/11-backend-catalogo-ubicaciones.md`](../especificacion/11-backend-catalogo-ubicaciones.md).
  25/25 tests, BUILD SUCCESS, sin bugs nuevos: menú público en 3 queries con
  cache corto + ETag, disponibilidad on/off inmediata, reglas de integridad
  reales (no diferidas), aislamiento cross-tenant probado explícitamente.

## Objetivo

Implementar lo que cada balneario administra para poder vender: su menú
(categorías y productos) y sus ubicaciones físicas de entrega. Es el módulo de
ABMs tenant-scoped por excelencia; sirve además para validar en serio el
aislamiento multitenant construido en la etapa 09.

## Alcance / Entregables

1. **Categorías de menú**: ABM con orden manual (el admin decide qué se ve
   primero — decisión comercial), activo/inactivo.
2. **Productos**: ABM con nombre, descripción, precio, foto, categoría, orden,
   y **disponibilidad on/off inmediata** (se terminó el hielo → el producto
   desaparece del menú del cliente al instante). Incluye **variantes/opciones**
   (confirmado en etapa 01): ABM de variantes simples de un nivel por producto
   (ej. tamaño o sabor) con precio propio y disponibilidad propia.
3. **Menú público del balneario**: endpoint optimizado para la app cliente
   (categorías ordenadas + productos disponibles + promociones vigentes
   embebidas o referenciadas), con cache corto y ETag — es el endpoint más
   consultado de toda la plataforma.
4. **Ubicaciones**: ABM (tipo carpa/sombrilla/mesa/sector, identificador
   visible tipo "Carpa 12", estado activa/inactiva). La validación de estadía
   es por carpero (decisión de etapa 01), así que no se requieren códigos QR
   por ubicación en el MVP.
5. **Reglas de integridad**: no borrar categoría con productos, no desactivar
   ubicación con estadía activa (o definir el comportamiento), soft-delete
   según convención de la etapa 03.

## Inputs requeridos

- Entregable de la etapa 01: variantes confirmadas (sí, un nivel) y validación
  de estadía por carpero (sin QR).

## Criterios de aceptación

- Un admin del balneario A no puede tocar catálogo ni ubicaciones del B (test
  cross-tenant explícito).
- Apagar la disponibilidad de un producto lo saca del menú público en la
  siguiente consulta.
- El menú público responde en una sola llamada todo lo que la pantalla de menú
  de la etapa 07 necesita.
