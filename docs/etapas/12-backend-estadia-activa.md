# Etapa 12 — Backend estadía activa

- **Orden:** 12
- **Modelo ejecutor:** opus
- **Tipo:** construcción
- **Depende de:** 09, 10, 11

## Objetivo

Implementar la estadía activa: el concepto de dominio distintivo de EasyBeach.
Es el vínculo persistente cliente–balneario–ubicación sobre el que cuelga todo
el consumo. Lo ejecuta opus porque concentra las reglas de negocio más finas y
los casos borde más delicados del producto.

## Alcance / Entregables

1. **Ciclo de vida completo** según la máquina de estados de la etapa 03:
   - **Apertura en dos pasos (decisión de etapa 01)**: el cliente solicita la
     apertura eligiendo balneario y ubicación → la estadía queda
     `PENDIENTE_VALIDACION` → un **carpero la confirma** desde el panel
     operativo y pasa a `ACTIVA` (o la rechaza). Hasta la validación no se
     habilitan pedidos. Valida: balneario activo y en temporada, ubicación
     válida, y regla de unicidad **por cliente y por balneario** (un cliente
     puede tener estadías activas en balnearios distintos a la vez, pero no
     dos en el mismo balneario — comportamiento explícito si intenta abrir
     otra en el mismo balneario: rechazar u ofrecer cerrar la anterior).
   - **Persistencia**: la estadía sobrevive días o toda la temporada; el
     re-ingreso a la app recupera la estadía activa sin fricción (endpoint
     "mi estadía actual").
   - **Cambio de ubicación** dentro del mismo balneario (hoy carpa 12, mañana
     carpa 15) preservando historial.
   - **Cierre explícito** por el cliente, con resumen de consumo total
     (cantidad de pedidos, monto acumulado). Reglas de cierre con pedidos en
     curso (bloquear o cancelar — según etapa 01/03).
2. **Casos borde resueltos y testeados**:
   - Balneario suspendido por Super Admin con estadías abiertas (política de la
     etapa 10).
   - Fin de temporada con estadías abiertas: job de cierre administrativo
     (estado `CERRADA_POR_SISTEMA` distinguible del cierre normal — importa
     para reportes).
   - Concurrencia: dos aperturas simultáneas del mismo cliente en el mismo
     balneario (constraint en DB sobre `cliente_id + balneario_id`, no solo
     validación en código).
   - Solicitudes de apertura que nunca son validadas por un carpero:
     expiración automática de estadías `PENDIENTE_VALIDACION` (definir TTL).
3. **Consultas**: estadía activa del cliente, historial de estadías del
   cliente, estadías activas por balneario/ubicación (insumo del panel
   operativo y de reportes).
4. **Eventos de dominio** (`EstadiaAbierta`, `EstadiaCerrada`) publicados
   internamente — la etapa 15 (reportes) y futuras features (ej. promo de
   bienvenida) se cuelgan de acá.

## Inputs requeridos

- Entregable de la etapa 01: unicidad por cliente+balneario y validación de
  apertura por carpero ya están decididas; queda por definir la política
  exacta de cierre con pedidos en curso (etapa 03) — si sigue abierta, el
  ejecutor la bloquea como pregunta antes de codificar.

## Criterios de aceptación

- Todas las transiciones de la máquina de estados tienen test; las transiciones
  inválidas devuelven error del formato estándar.
- El constraint de unicidad de estadía activa (`cliente_id + balneario_id`)
  está en la base de datos y hay un test de concurrencia que lo demuestra; un
  test verifica que el mismo cliente sí puede tener estadías activas en dos
  balnearios distintos.
- Una estadía `PENDIENTE_VALIDACION` no permite crear pedidos (test), y la
  validación del carpero queda registrada con actor y timestamp.
- El resumen de cierre cuadra con la suma de pedidos entregados de la estadía.
