# Etapa 08 — Diseño UX/UI web (paneles)

- **Orden:** 08
- **Modelo ejecutor:** claude design (diseño visual — se hace a mano)
- **Tipo:** diseño
- **Depende de:** 01, 06

## Objetivo

Diseñar los tres paneles web (React/Next.js). El panel operativo es una
herramienta de trabajo bajo presión en temporada: la métrica es velocidad de
despacho, no belleza. Los paneles de administración priorizan claridad y
autonomía del balneario.

## Alcance / Entregables

1. **Panel operativo (barra/cocina/carpero)** — el crítico:
   - Cola de pedidos entrantes en vivo, con ubicación de entrega bien visible,
     antigüedad del pedido y alerta de demora.
   - Transición de estados en un click/tap grande (pensado para tablet con manos
     mojadas y apuro).
   - Cola separada de solicitudes de servicio al carpero.
   - **Bandeja de validación de estadías** (decisión de etapa 01): solicitudes
     de apertura pendientes con cliente y ubicación, confirmar/rechazar en un
     tap.
   - Sonido/notificación al entrar un pedido nuevo.
   - Diseño responsive: tablet como dispositivo primario.
2. **Panel admin de balneario**:
   - Dashboard con las métricas que importan al dueño: facturación del día,
     pedidos, ticket promedio, productos más vendidos (conecta directo con el
     objetivo "vender más").
   - ABM de menú (categorías, productos **con variantes**, fotos,
     disponibilidad on/off rápida), ubicaciones, promociones (creación guiada
     por tipo), usuarios staff.
   - Configuración visual del balneario — **theme white-label completo**
     (paleta, logo, tipografía del set curado, portada, splash) — con
     **preview en vivo** de cómo la ve el cliente.
   - Estado de la **vinculación con Mercado Pago** (OAuth): conectar cuenta,
     ver estado, re-vincular; advertencia visible si no está conectada (sin
     ella el balneario no cobra).
   - Reportes básicos con filtro por fecha y export.
3. **Panel Super Admin**:
   - ABM de balnearios, planes y temporadas; activar/suspender balneario;
     estado de la suscripción por temporada. Utilitario, sin theming por tenant.
4. **Transversal**: login por rol, layout/navegación de cada panel, estados
   vacíos (balneario recién creado: qué ve el admin y qué le sugiere hacer
   primero — onboarding hacia su primer menú publicado).

## Inputs requeridos

- Design system (etapa 06).
- Máquinas de estado de pedido/servicio (etapa 03) para diseñar las
  transiciones exactas del panel operativo.
- Lista de reportes básicos del MVP (etapa 01/15): confirmar los 3–5 reportes.

## Criterios de aceptación

- En el panel operativo, pasar un pedido al siguiente estado toma 1 interacción
  y es legible a 2 metros en una tablet.
- Todo lo configurable por balneario en el modelo de datos tiene su pantalla de
  administración.
- El flujo "balneario nuevo → primer menú publicado" está diseñado de punta a
  punta.
