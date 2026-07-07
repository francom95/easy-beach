# Etapa 01 — Visión, alcance MVP y glosario de dominio

- **Estado:** decisiones de negocio cerradas — listo como insumo para etapas 02–05.
- **Corresponde al plan:** [docs/etapas/01-vision-alcance-glosario.md](../etapas/01-vision-alcance-glosario.md)

## 1. Visión de producto

**Problema.** En un balneario, el consumo del cliente está limitado por la fricción
física: para pedir algo hay que levantarse, ir hasta la barra, hacer fila, volver.
Cada viaje que el cliente no hace es una venta que el balneario no concreta. Los
pedidos de servicio al carpero (sombra, reposeras) dependen de cruzarlo caminando
por la playa. El balneario no tiene visibilidad agregada de qué vende, cuándo y a
quién.

**Propuesta de valor.** EasyBeach elimina la fricción entre "tener ganas de pedir"
y "pedir": el cliente abre una estadía en su ubicación (carpa/sombrilla/mesa) y
desde ahí pide comida, bebida, productos y servicios sin moverse, ve promociones
activas y paga desde la app. El balneario gana un canal de venta adicional que
opera solo, con datos de consumo en tiempo real.

**Objetivo rector.** Que los balnearios vendan más. Cada decisión de producto de
este documento se juzga contra esa vara: ¿esto reduce fricción de compra o la
aumenta?

**Métricas de éxito del MVP:**
- Pedidos por estadía (objetivo: más de 1 pedido adicional promedio vs. no tener
  la app).
- Ticket promedio por estadía.
- % de pedidos vía app vs. mostrador físico (canal paralelo, no reemplazado en
  MVP).
- Tiempo entre apertura de estadía y primer pedido.
- Tasa de abandono de carrito (fricción de checkout/pago).

## 2. Decisiones de negocio resueltas

Estas eran las preguntas abiertas señaladas en el plan de la etapa 01. Quedaron
resueltas así:

| # | Pregunta | Decisión | Impacto directo |
|---|---|---|---|
| 1 | ¿El MVP incluye pagos? | **Sí. Pago online in-app vía Mercado Pago, con Checkout API, en modelo marketplace/split SIN comisión de plataforma:** cada balneario cobra en su **propia cuenta de Mercado Pago** (vinculada por OAuth al darse de alta), EasyBeach envía `application_fee = 0` (no retiene nada) y en el resumen de tarjeta del cliente aparece el descriptor del balneario, nunca una línea de EasyBeach. El cliente paga al confirmar cada pedido (no hay cuenta corriente de estadía en el MVP). La única comisión existente es la propia de Mercado Pago por procesar, que paga el balneario como en cualquier venta directa. | Etapas 02 (ADR de integración de pagos), 03 (entidad de pago), 04 (endpoints de checkout + webhook), 05 (seguridad del webhook y custodia de tokens OAuth), 10 (paso de vinculación de cuenta MP en el alta del balneario). |
| 2 | ¿Cómo se valida la apertura de estadía / ubicación? | **Validada por el carpero.** El cliente solicita apertura de estadía en una ubicación; un carpero la confirma desde el panel operativo antes de que queden habilitados los pedidos. | Etapas 03 (estado intermedio de estadía "pendiente de validación"), 07/08 (pantalla de espera de confirmación), 12 (backend), 17 (bandeja de validación del carpero). |
| 3 | ¿Un cliente puede tener estadías activas en más de un balneario a la vez? | **Sí, una estadía activa por balneario** (no una única estadía global). La unicidad se valida por el par cliente+balneario. | Etapa 03 (constraint de unicidad `cliente_id + balneario_id`, no solo `cliente_id`), 07 (la app debe dejar claro "en qué balneario estás pidiendo" si hay más de una estadía abierta). |
| 4 | ¿El catálogo incluye variantes de producto? | **Sí, incluye variantes** (ej. tamaño, con/sin hielo, tipo de carne). | Etapas 03 (entidad de variante/opción y su precio), 04 (contrato de producto con variantes), 11 (ABM de variantes), 13 (precio congelado por variante elegida, no por producto base). |
| 5 | ¿Qué alcance tiene la identidad visual del balneario dentro de la app? | **Tematización total (white-label).** Al seleccionar un balneario, **toda la estética de la app se transforma como si la app fuera del balneario**: colores, logo, tipografía (de un set curado), imágenes, splash y pantallas completas. La marca EasyBeach solo es visible antes de la selección de balneario (onboarding/selector) y en los paneles web internos; el cliente en estadía vive una app 100 % del balneario. | Etapas 06 (design system: el set de tokens debe cubrir la capa visual completa, no solo logo y dos colores), 07 (UX de transición de marca al elegir balneario), 10 (la configuración visual servida por API abarca el theme completo), 16 (theming runtime integral en mobile). |

## 3. Glosario de dominio

- **Balneario (tenant):** unidad de negocio con identidad visual, menú,
  ubicaciones, usuarios y operación propia. Cada balneario opera de forma
  aislada dentro de la plataforma.
- **Tematización por balneario (white-label):** transformación completa de la
  estética de la app al seleccionar un balneario. No es un "acento de color":
  la app adopta íntegramente la identidad del balneario (paleta completa,
  logo, tipografía de un set curado, imágenes de portada, splash) y se
  percibe como la app propia del balneario. La marca EasyBeach solo aparece
  antes de la selección (onboarding/selector de balneario) y en los paneles
  web de staff.
- **Estadía activa:** vínculo cliente–balneario–ubicación, abierto por el
  cliente y **confirmado por un carpero**, que persiste hasta cierre explícito
  del cliente. Puede durar un día o toda la temporada. Regla de unicidad:
  **una estadía activa por cliente y por balneario** (un mismo cliente puede
  tener, a la vez, una estadía activa en el Balneario A y otra en el Balneario
  B, pero no dos abiertas simultáneamente dentro del mismo balneario).
- **Ubicación:** punto físico de entrega (carpa, sombrilla, mesa, sector).
- **Carpero:** personal operativo que recibe solicitudes de servicio y valida
  la apertura de estadías en su balneario.
- **Producto:** ítem vendible del menú. Puede tener una o más **variantes**.
- **Variante / opción de producto:** dimensión configurable de un producto
  (ej. tamaño, sabor, con/sin hielo) que puede ajustar el precio. El precio
  final de un ítem de pedido se congela sobre la variante elegida, no sobre el
  producto base.
- **Pedido:** orden de productos (con sus variantes elegidas) con ciclo de
  estados y un **pago asociado** procesado vía Mercado Pago.
- **Pago:** transacción vinculada a un pedido, iniciada mediante Mercado Pago
  Checkout API. Un pedido no se considera confirmado operativamente hasta que
  su pago queda aprobado.
- **Servicio al carpero:** solicitud sin productos (ej. sombra, reposera,
  limpieza), con su propio ciclo simple. Sin cobro en el MVP (ver alcance).
- **Promoción / Combo:** mecánica de incentivo de consumo (descuento %, combo
  a precio fijo, happy hour por franja horaria).
- **Temporada / Plan:** unidad comercial del SaaS gestionada por Super Admin;
  determina si un balneario está operativo.

## 4. Actores y roles

| Actor | Rol en el sistema |
|---|---|
| **Cliente** | Elige balneario, abre estadía (pendiente de validación), pide, paga, sigue sus pedidos, solicita servicios, cierra su estadía. |
| **Carpero** | Valida aperturas de estadía en su ubicación, recibe y resuelve solicitudes de servicio. |
| **Operador de barra/cocina** | Recibe y despacha pedidos (cambia sus estados). |
| **Admin de balneario** | Configura menú, variantes, ubicaciones, promociones, staff, identidad visual; ve reportes. |
| **Super Admin** | Da de alta balnearios, gestiona planes y temporadas, activa/suspende balnearios. |

## 5. Alcance MVP explícito

**Incluido:**
- Multibalneario, ABM de balnearios.
- **Tematización white-label por balneario**: al elegir balneario, la app
  cliente adopta su identidad visual completa (ver glosario); configurable
  por el admin del balneario con preview.
- Selección de balneario por el cliente.
- Vinculación de la cuenta de Mercado Pago propia de cada balneario (OAuth)
  como paso del alta/onboarding del balneario.
- Estadía activa por balneario, con apertura validada por el carpero.
- Menú con categorías, productos **y variantes**.
- Carrito y confirmación de pedido con **pago online vía Mercado Pago
  Checkout API** (pago por pedido, no cuenta corriente).
- Pedidos por ubicación, con estados y seguimiento en tiempo real.
- Servicios al carpero (sin cobro en el MVP).
- Promociones básicas (descuento %, combo a precio fijo, happy hour).
- Paneles admin (balneario) y operativo (staff), y panel Super Admin.
- Reportes básicos (ventas, productos más vendidos, promociones, estadías).

**Excluido del MVP (a reevaluar post-MVP):**
- Cuenta corriente de estadía (pago acumulado a saldar al cierre).
- Reservas anticipadas de ubicación (antes de llegar al balneario).
- Delivery fuera del predio del balneario.
- Multiidioma.
- Cobro de servicios al carpero (quedan sin precio; monetizarlos es una
  decisión futura).
- Combos/variantes anidados o configurables en múltiples niveles: el MVP
  soporta variantes simples de un nivel por producto (ej. tamaño *o* sabor,
  no ambos combinados dinámicamente).

## 6. User journeys de alto nivel

**(a) Cliente — ciclo completo de una visita.**
Llega al balneario → abre la app → elige balneario → **la app se transforma
por completo a la identidad visual del balneario (white-label)** → indica su ubicación
(carpa/sombrilla/mesa) → la estadía queda **pendiente de validación** → un
carpero la confirma desde el panel operativo → el cliente ya puede pedir →
arma carrito con productos y variantes → confirma pedido → paga vía Mercado
Pago → sigue el estado del pedido en tiempo real → lo recibe → repite este
ciclo durante el día o los días que dure su estadía → cierra la estadía
cuando se va, con resumen de consumo total.

**(b) Cliente con estadías en dos balnearios distintos en la misma
temporada.** Un cliente puede, en fechas distintas (o incluso el mismo día,
si el modelo de negocio lo permite), tener una estadía activa en el
Balneario A y, por separado, una estadía activa en el Balneario B. La app
debe dejar claro en todo momento en qué balneario/estadía está pidiendo.

**(c) Carpero — validación y servicio.** Ve solicitudes de apertura de
estadía pendientes en su balneario, confirma la ubicación real del cliente,
y por separado atiende su cola de solicitudes de servicio (sombra, reposera,
etc.).

**(d) Operador de barra/cocina.** Ve la cola de pedidos pagados y confirmados
de su balneario, ordenada por antigüedad, y transiciona cada uno por su
ciclo de estados hasta la entrega.

**(e) Admin de balneario.** Configura menú (categorías, productos,
variantes, precios, fotos), ubicaciones, promociones, identidad visual, y
consulta reportes de venta y desempeño.

**(f) Super Admin.** Da de alta un balneario nuevo (crea su primer admin),
lo suscribe a un plan/temporada, y puede suspenderlo si corresponde.

## 7. Notas de impacto para las etapas siguientes

La incorporación de pagos con Mercado Pago y de variantes de producto no
estaba resuelta cuando se escribió el plan original (etapas 02–05). Quedan
estos puntos para que las etapas correspondientes los tomen como insumo:

- **Etapa 02 (Arquitectura):** agregar un ADR de integración de pagos:
  creación de preferencia/orden de pago, recepción de confirmación vía
  **webhook de Mercado Pago**, y cómo esa confirmación asíncrona interactúa
  con la máquina de estados del pedido (un pedido no pasa a `CONFIRMADO`
  hasta que el webhook marca el pago como aprobado).
- **Etapa 03 (Modelo de datos):** agregar entidad de variante/opción de
  producto con su propio precio; agregar entidad de pago (`pedido_pago`) con
  estado (`pendiente`, `aprobado`, `rechazado`, `reembolsado`) y referencias
  externas de Mercado Pago (`preference_id`, `payment_id`); cambiar el
  constraint de unicidad de estadía activa de `cliente_id` a
  `cliente_id + balneario_id`; agregar estado intermedio de estadía
  `PENDIENTE_VALIDACION` antes de `ACTIVA`.
- **Etapa 04 (Contratos de API):** documentar el endpoint de creación de
  preferencia de pago y el endpoint receptor del webhook de Mercado Pago
  (incluyendo su formato de payload); documentar el endpoint de validación
  de estadía por el carpero.
- **Etapa 05 (Seguridad):** validar la autenticidad de las notificaciones
  webhook de Mercado Pago (firma/secret); no almacenar datos de tarjeta (los
  tokeniza Mercado Pago del lado de Checkout API, fuera del alcance PCI de
  EasyBeach).

- **Etapa 06 (Design system):** el sistema de theming debe diseñarse para
  **white-label total**, no para personalización parcial: el set de tokens
  cubre la capa visual completa (paleta íntegra, tipografía de un set
  curado, imágenes, splash), y lo que queda común (estructura de navegación,
  componentes, patrones de interacción) debe ser visualmente neutro para que
  la marca percibida sea 100 % la del balneario.
- **Etapa 10 (Backend Super Admin / alta de balneario):** el onboarding del
  balneario incluye un paso obligatorio de **vinculación de su cuenta de
  Mercado Pago vía OAuth**; sin cuenta vinculada, el balneario no puede
  recibir pedidos pagos. La configuración visual servida por API abarca el
  theme completo.
- **Etapa 16 (Mobile):** el theming runtime aplica la identidad completa; a
  partir de la selección de balneario no queda marca EasyBeach visible en la
  UI del cliente.

**Decisión de pagos cerrada (antes pendiente):** modelo
**marketplace / split payments de Mercado Pago, sin comisión de
plataforma**. Cada balneario cobra en su propia cuenta de Mercado Pago
(vinculada por OAuth; EasyBeach guarda el `access_token` de cada balneario
y crea los pagos contra esa cuenta), con `application_fee = 0`: EasyBeach no
retiene ni comisiona nada, y no aparece ninguna línea de EasyBeach en el
resumen de tarjeta del cliente — el descriptor es el del balneario. La única
comisión es la estándar de Mercado Pago por procesamiento, a cargo del
balneario como en una venta directa. *Nota operativa:* confirmar con el
área comercial de Mercado Pago, al registrar la aplicación como marketplace,
que no haya requisitos de proceso adicionales para operar con
`application_fee = 0` (no hay costo esperado, solo validación de proceso).
