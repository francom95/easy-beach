# Etapa 15 — Backend reportes básicos

- **Orden:** 15
- **Modelo ejecutor:** haiku
- **Tipo:** construcción
- **Depende de:** 13, 14

## Objetivo

Implementar los reportes básicos del MVP: consultas de solo lectura sobre datos
que ya existen. Trabajo bien delimitado y mecánico — por eso haiku — pero de
alto valor comercial: es lo que le demuestra al dueño del balneario que la
plataforma le hace vender más.

## Alcance / Entregables

1. **Reportes por balneario** (admin), todos con filtro por rango de fechas:
   - Ventas: facturación total, cantidad de pedidos, ticket promedio, por día.
   - Productos más vendidos (unidades y facturación).
   - Rendimiento de promociones: usos y monto descontado por promoción.
   - Estadías: abiertas por día, duración promedio, consumo promedio por
     estadía.
   - Servicios al carpero: solicitudes por tipo y tiempos de resolución.
2. **Resumen para dashboard** (etapa 08): endpoint único con los KPIs del día
   (facturación, pedidos, ticket promedio, pedidos en curso).
3. **Reporte de plataforma** (Super Admin): balnearios activos, volumen de
   pedidos por balneario en la temporada.
4. **Export CSV** de cada reporte.
5. **Implementación**: queries de solo lectura optimizadas (usar los índices de
   la etapa 03); sin tablas de agregación ni jobs en MVP — si una query no
   rinde, escalar la decisión, no inventar infraestructura.

## Inputs requeridos

- Lista cerrada de reportes confirmada (etapas 01 y 08). Esta etapa NO decide
  qué reportar; solo implementa lo definido.

## Criterios de aceptación

- Cada reporte tiene test con dataset conocido y resultado verificado a mano.
- Los montos cuadran con los pedidos entregados (excluyen cancelados; criterio
  documentado en cada reporte).
- Filtros de fecha respetan la zona horaria de la etapa 04.
- Ningún reporte cruza datos entre balnearios salvo el de Super Admin.
