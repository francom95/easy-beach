# Etapa 18 — Web: panel Super Admin

- **Orden:** 18
- **Modelo ejecutor:** haiku
- **Tipo:** construcción
- **Depende de:** 08 (diseño), 10 (API)
- **Puede correr en paralelo con:** 16 y 17

## Objetivo

Construir el panel del Super Admin dentro de la misma app Next.js (o como
sección separada, según lo que haya definido la etapa 08). Es un panel
utilitario de bajo riesgo y CRUDs directos sobre la API de la etapa 10 — por
eso haiku.

## Alcance / Entregables

1. **Login de Super Admin** y rutas protegidas exclusivas del rol.
2. **ABM de balnearios**: alta con creación del primer admin, edición,
   activar/suspender con confirmación (la suspensión tiene efecto operativo
   inmediato — mostrar advertencia).
3. **Planes y temporadas**: ABM de planes, definición de temporadas,
   suscripción de balnearios a plan por temporada, estado de cada suscripción.
4. **Vista de plataforma**: listado de balnearios con estado, plan y volumen
   básico (reporte de la etapa 15).
5. **Auditoría**: vista de las acciones registradas en la etapa 10.

## Inputs requeridos

- Mockups del panel Super Admin (etapa 08).
- API de la etapa 10 (y el reporte de plataforma de la 15 para la vista de
  volumen; puede stubearse si la 15 no terminó).

## Criterios de aceptación

- El flujo comercial completo se opera desde la UI: alta de balneario →
  suscripción a temporada → el balneario aparece en la app del cliente.
- Suspender un balneario desde el panel lo saca de la app del cliente.
- Ningún otro rol accede a estas rutas (test).
