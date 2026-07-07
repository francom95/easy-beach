# Etapa 01 — Visión, alcance MVP y glosario de dominio

- **Orden:** 01
- **Modelo ejecutor:** sonnet
- **Tipo:** especificación (documento, sin código)
- **Depende de:** —
- **Estado: ✅ EJECUTADA** — entregable en
  [`docs/especificacion/01-vision-alcance-glosario.md`](../especificacion/01-vision-alcance-glosario.md).
  Decisiones cerradas: pagos in-app vía **Mercado Pago Checkout API en modelo
  marketplace sin comisión** (cada balneario cobra en su propia cuenta MP vía
  OAuth, `application_fee = 0`); apertura de estadía **validada por carpero**;
  una estadía activa **por cliente y por balneario**; catálogo **con
  variantes**; y **theming white-label total** (al elegir balneario, toda la
  estética de la app pasa a ser la del balneario).

## Objetivo

Fijar el lenguaje común del proyecto y el alcance exacto del MVP, para que todas
las etapas posteriores usen los mismos términos y no haya ambigüedad sobre qué
entra y qué no.

## Alcance / Entregables

1. **Documento de visión de producto** (1–2 páginas): problema, propuesta de
   valor, objetivo rector (los balnearios venden más), métricas de éxito del MVP
   (ej.: pedidos por estadía, ticket promedio, % de pedidos vía app vs. mostrador).
2. **Glosario de dominio** con definición operativa de cada término. Mínimo:
   - **Balneario** (tenant): unidad de negocio con identidad visual, menú,
     ubicaciones, usuarios y operación propia.
   - **Estadía activa**: vínculo cliente–balneario–ubicación que persiste hasta
     cierre explícito por el cliente; puede durar un día o toda la temporada.
     Solo una estadía activa por cliente a la vez (confirmar regla).
   - **Ubicación**: punto físico de entrega (carpa, sombrilla, mesa, sector).
   - **Carpero**: personal operativo que recibe pedidos de servicio.
   - **Pedido**: orden de productos con ciclo de estados.
   - **Servicio al carpero**: solicitud sin productos (ej. sombra, reposera,
     limpieza), con su propio ciclo simple.
   - **Promoción / Combo**: mecánica de incentivo de consumo.
   - **Temporada / Plan**: unidad comercial del SaaS gestionada por Super Admin.
3. **Actores y roles** (versión producto, no técnica): Cliente, Carpero,
   Operador de barra/cocina, Admin de balneario, Super Admin.
4. **Alcance MVP explícito** (in/out): lista cerrada de features MVP
   (multibalneario, ABM de balnearios, configuración visual, selección de
   balneario, estadía activa, menú, carrito, pedidos por ubicación, estados de
   pedido, servicios al carpero, promociones básicas, panel admin, panel
   operativo, reportes básicos) y lista de exclusiones (ej.: pagos online,
   reservas de carpa anticipadas, delivery fuera del balneario, multiidioma —
   confirmar cada exclusión).
5. **User journeys de alto nivel** (texto): (a) cliente llega, elige balneario,
   abre estadía, pide y consume durante días; (b) operador recibe y despacha
   pedidos; (c) admin configura su balneario; (d) super admin da de alta un
   balneario nuevo.

## Inputs requeridos

- Decisiones de negocio pendientes que el ejecutor debe plantear como preguntas
  si no están resueltas: ¿el MVP incluye pagos (online / a cuenta / efectivo al
  entregar)? ¿la estadía requiere validación del balneario o la abre el cliente
  solo? ¿un cliente puede tener estadías en dos balnearios a la vez?

## Criterios de aceptación

- Todo término del glosario tiene una definición de una frase + reglas de negocio.
- El alcance MVP es una lista cerrada verificable (cada ítem es demostrable).
- No queda ninguna pregunta de negocio sin respuesta o sin dueño asignado.
