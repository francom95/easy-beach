# Etapa 07 — Diseño UX/UI app mobile (cliente)

- **Orden:** 07
- **Modelo ejecutor:** claude design (diseño visual — se hace a mano)
- **Tipo:** diseño
- **Depende de:** 01, 06

## Objetivo

Diseñar todas las pantallas de la app del cliente (React Native). Es la
superficie que genera la venta: cada decisión de UX se evalúa por "¿hace que el
cliente pida más y con menos fricción?".

## Alcance / Entregables

Flujos y pantallas completas (mockups de alta fidelidad + mapa de navegación):

1. **Onboarding y selección de balneario**: registro/login liviano, elección de
   balneario (lista/búsqueda; evaluar geolocalización). Es la única zona de la
   app con marca EasyBeach. Al confirmar el balneario ocurre la **transición
   white-label** (etapa 06): la app se transforma por completo a la identidad
   del balneario — desde ese momento se ve y se siente como la app propia del
   balneario, sin rastro visual de EasyBeach.
2. **Apertura de estadía**: elegir ubicación (carpa/sombrilla/mesa) y
   solicitar apertura. Decisión de etapa 01: la valida el carpero — diseñar la
   **pantalla de espera de confirmación** (estado `PENDIENTE_VALIDACION`) y el
   momento en que la estadía queda activa y se habilita el pedido.
3. **Home de estadía activa** — la pantalla más importante: acceso inmediato a
   menú, promociones destacadas, estado de pedidos en curso, botón de servicio
   al carpero. La estadía persiste días: diseñar el re-ingreso diario (abrís la
   app y estás "en tu carpa").
4. **Menú y producto**: categorías, cards con foto y precio, detalle de
   producto, agregado al carrito con cantidad. Badges de promoción visibles en
   el listado (la promo tiene que encontrarte, no al revés).
5. **Carrito, pago y confirmación**: edición, selección de variantes,
   subtotal, aplicación de promociones/combos, confirmación con ubicación de
   entrega visible, **pago in-app vía Mercado Pago (Checkout API)** con sus
   estados (procesando, aprobado, rechazado con reintento), y feedback claro
   de pedido pagado y confirmado.
6. **Seguimiento de pedidos**: estados en tiempo real con lenguaje humano
   ("lo están preparando", "va en camino"), historial de la estadía.
7. **Servicios al carpero**: solicitud en 2 taps (tipos predefinidos), estado.
8. **Promociones**: sección propia + integración transversal (home, menú,
   carrito).
9. **Cierre de estadía**: resumen de consumo total (refuerza percepción de
   valor para el balneario), cierre explícito, estado post-cierre.
10. **Estados no felices**: sin conexión (playa), balneario cerrado/fuera de
    temporada, producto no disponible, pedido cancelado por el local.

## Inputs requeridos

- Design system y tokens de theming (etapa 06).
- User journeys (etapa 01) y decisión de validación de estadía (etapa 05).
- Contratos de API (etapa 04) como referencia de datos disponibles por pantalla
  (deseable, no bloqueante).

## Criterios de aceptación

- Mapa de navegación completo; ninguna pantalla huérfana.
- **Test white-label:** desde la selección de balneario en adelante, ninguna
  pantalla muestra marca EasyBeach; toda la estética es la del balneario.
- Del home de estadía a "pedido confirmado" en ≤ 4 taps para un producto simple.
- Cada pantalla tiene definidos: estado con datos, vacío, cargando y error.
- Los mockups usan tokens del design system (ningún color hardcodeado fuera
  del sistema).
