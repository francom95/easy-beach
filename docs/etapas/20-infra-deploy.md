# Etapa 20 — Infraestructura, deploy y observabilidad

- **Orden:** 20
- **Modelo ejecutor:** sonnet
- **Tipo:** construcción
- **Depende de:** 09 (puede arrancar apenas exista backend); cierre final tras 19

## Estado: ✅ EJECUTADA

Ver [`docs/especificacion/20-infra-deploy.md`](../especificacion/20-infra-deploy.md)
para el detalle completo. Resumen: Dockerfiles + docker-compose verificados
con un `docker compose up` real (2 bugs reales encontrados y arreglados: JWT
key loading, healthcheck IPv4/IPv6 de `web`); CI extendido (web-ci, deploy
manual-gatillado, doc de mobile signing); backup/restore de DB probado de
verdad; alertas mínimas verificadas con `promtool` en una prueba controlada;
runbook de incidentes; documento de escalado estacional. Suite backend:
151/151.

## Objetivo

Llevar EasyBeach a producción de forma operable por un equipo chico: entornos,
deploy repetible, backups y monitoreo. El negocio es estacional: la
infraestructura debe aguantar el pico de enero/febrero sin sobredimensionar el
resto del año.

## Alcance / Entregables

1. **Decisión de hosting documentada (ADR)**: evaluar opciones con criterio de
   costo en ARS/USD y simplicidad operativa (VPS + Docker Compose vs. PaaS vs.
   cloud administrado). Para el MVP alcanza con algo simple y bien respaldado;
   justificar.
2. **Entornos**: staging (lo usa la etapa 19) y producción, con paridad de
   configuración; secretos fuera del repo; dominios y TLS.
3. **CI/CD**: pipeline que construye, corre tests y despliega backend y web;
   builds firmados de mobile y publicación en Google Play / App Store
   (cuentas, fichas de store, proceso de review — incluir tiempos de Apple en
   el plan).
4. **Base de datos**: backups automáticos con restore probado (un backup no
   verificado no es un backup), plan de migraciones en deploy sin downtime
   razonable.
5. **Observabilidad**: logs centralizados con tenant y request-id (etapa 09),
   métricas y alertas mínimas (API caída, tasa de errores, latencia del menú
   público, espacio en disco), uptime check externo. Runbook de incidentes de
   1 página: qué mirar y qué reiniciar a las 15:00 de un sábado de enero.
6. **Estacionalidad**: documento corto de escalado para temporada (qué se sube
   antes de diciembre, qué se apaga en abril) alineado al modelo SaaS por
   temporada.

## Inputs requeridos

- Presupuesto mensual objetivo de infraestructura (si no hay dato, proponer
  tres escenarios de costo).
- Cuentas de Google Play y Apple Developer (tramitarlas temprano: la de Apple
  demora).

## Criterios de aceptación

- Deploy a producción reproducible desde CI (sin pasos manuales indocumentados).
- Restore de backup ejecutado y verificado al menos una vez.
- Las alertas mínimas dispararon en una prueba controlada.
- La app está publicada (o en review) en ambas stores y la web accesible por
  dominio propio con TLS.
