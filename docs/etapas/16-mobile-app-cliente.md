# Etapa 16 — Mobile app cliente (React Native)

- **Orden:** 16
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 07 (diseño), 09–14 (API)
- **Puede arrancar** su base (proyecto, navegación, theming, auth) apenas
  existan las etapas 07 y 09, en paralelo con el resto del backend.
- **Estado: ✅ EJECUTADA**, con verificación en dispositivo parcial — código
  en [`mobile/`](../../mobile/) (React Native 0.86 bare, TypeScript),
  entregable en
  [`docs/especificacion/16-mobile-app-cliente.md`](../especificacion/16-mobile-app-cliente.md).
  `tsc` limpio, suite backend completa verde. Hallazgo crítico: una carrera
  real entre la promesa del theme y la navegación a S05 dejaba
  `balnearioSlug` en `null` — encontrado en vivo contra un balneario
  bootstrapeado en el emulador, corregido. Verificación visual completa
  bloqueada por inestabilidad del emulador bajo carga combinada de
  MySQL+backend+Metro en este entorno (detalle en el entregable §5).

## Objetivo

Construir la app del cliente (Android/iOS) implementando los diseños de la
etapa 07 contra la API real. Es la superficie de venta: la calidad percibida
acá define la adopción.

## Alcance / Entregables

1. **Base técnica**: proyecto React Native, navegación, cliente HTTP con
   manejo de tokens (refresh automático — sesiones largas de temporada),
   almacenamiento seguro de credenciales, gestión de estado, manejo de errores
   global coherente con el formato de la API.
2. **Theming dinámico multibalneario (white-label total)**: el sistema de
   tokens de la etapa 06 aplicado en runtime desde el endpoint de branding
   (etapa 10), cubriendo la identidad completa — paleta íntegra, logo,
   tipografía del set curado, splash/transición de entrada, imágenes — de modo
   que, elegido el balneario, la app se percibe como la app propia del
   balneario y no queda marca EasyBeach visible. Cache local del theme y
   fallback al theme neutro EasyBeach solo fuera de una estadía/selección.
3. **Todos los flujos de la etapa 07**: onboarding y selección de balneario,
   solicitud de apertura de estadía con espera de validación del carpero, home
   de estadía activa, menú y detalle de producto con variantes, carrito y
   confirmación de pedido con **pago in-app vía Mercado Pago Checkout API**
   (con clave de idempotencia y manejo de pago rechazado/reintento),
   seguimiento en tiempo real (canal de la etapa 02 + fallback polling),
   servicios al carpero, promociones, cierre de estadía con resumen.
4. **Tolerancia a conectividad de playa**: estados offline en cada pantalla,
   reintentos con la idempotencia de la etapa 13, cache del menú.
5. **Notificaciones push** para cambios de estado de pedido (si el ADR de la
   etapa 02 las incluyó en MVP; si no, in-app only y dejarlo explícito).
6. **Preparación de release**: iconos, splash, configuración de builds Android
   e iOS (sin publicar en stores todavía — eso es de la etapa 20).

## Inputs requeridos

- Mockups y mapa de navegación de la etapa 07.
- API de las etapas 09–14 desplegada en un entorno accesible (o mocks desde la
  spec OpenAPI de la etapa 04 para arrancar antes).

## Criterios de aceptación

- El flujo completo (elegir balneario → abrir estadía → pedir → seguir estado →
  recibir → cerrar estadía) corre contra la API real en Android e iOS.
- Cambiar de balneario cambia la identidad visual completa sin recompilar, y
  tras la selección no queda ningún elemento de marca EasyBeach en la UI
  (test white-label de la etapa 06).
- Modo avión a mitad de un pedido: la app no duplica el pedido ni queda en
  estado inconsistente al reconectar.
- Paridad razonable con los mockups de la etapa 07 (revisión visual del diseñador).
