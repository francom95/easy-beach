# Etapa 14 — Backend servicios al carpero y promociones

- **Orden:** 14
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 12, 13

## Objetivo

Completar la superficie funcional del MVP con dos módulos que se apoyan en lo ya
construido: solicitudes de servicio al carpero (ciclo simple, sin dinero) y
promociones básicas (palanca directa del objetivo "vender más").

## Alcance / Entregables

### Servicios al carpero

1. **Catálogo de tipos de servicio** por balneario (ej.: armar sombra, reposera
   extra, limpieza, hielo) administrable por el admin — lista simple, sin precio
   en MVP (confirmar con etapa 01 si algún servicio se cobra).
2. **Solicitud**: cliente con estadía activa crea solicitud (tipo + nota
   opcional); queda ligada a su ubicación actual.
3. **Ciclo de estados simple** (etapa 03): `PENDIENTE → EN_CURSO → RESUELTA`,
   más `CANCELADA`. Cola para el carpero por antigüedad, misma mecánica de
   tiempo real que los pedidos.

### Promociones básicas

4. **Tipos de promoción del MVP** (según etapa 01/03):
   - Descuento porcentual sobre producto/categoría.
   - Combo a precio fijo (N productos por $X).
   - Franja horaria (happy hour) y vigencia por fechas.
5. **ABM de promociones** por admin de balneario, con activación on/off.
6. **Cálculo y aplicación**: implementar contra la interfaz que dejó lista la
   etapa 13; reglas de combinación (¿se acumulan promociones? — decisión simple
   para MVP: no se acumulan, gana la mejor para el cliente; confirmar).
7. **Exposición al cliente**: promociones vigentes en el menú público (etapa 11)
   y sección de promociones (etapa 07).

## Inputs requeridos

- Tipos de promoción confirmados y regla de combinación (etapa 01).
- Interfaz de cálculo de promociones de la etapa 13.

## Criterios de aceptación

- Cada tipo de promoción tiene tests de cálculo (incluyendo bordes de vigencia
  horaria y de fechas, con la TZ correcta).
- Una promoción vencida o desactivada no aparece en el menú ni se aplica a
  pedidos nuevos, pero los pedidos históricos conservan el descuento aplicado.
- El flujo de solicitud de servicio corre de punta a punta con su tiempo real.
