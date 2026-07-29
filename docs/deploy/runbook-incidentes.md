# Runbook de incidentes (1 página)

Escenario de referencia: son las 15:00 de un sábado de enero, el balneario
está lleno, y algo dejó de andar.

## 1. ¿Qué está roto? (2 minutos)

- **¿El panel web no carga?** → `https://<WEB_DOMAIN>/` en el navegador. Si
  tira error de TLS o "no se puede conectar": Caddy o DNS. Si carga pero las
  pantallas quedan en loading: el backend.
- **¿La app del cliente no muestra el menú?** → probar
  `https://<API_DOMAIN>/api/v1/balnearios/<slug>/menu` directo. 5xx o timeout
  → backend o MySQL. 404 → slug mal / balneario mal configurado (no es un
  incidente de infra).
- **¿Los pagos no confirman?** → revisar si el problema es el webhook de
  Mercado Pago (`docker compose logs backend | grep -i mercadopago`) antes de
  asumir que es infra propia - MP puede estar teniendo un incidente ellos.

## 2. Mirar esto en orden

```bash
docker compose ps                          # ¿algún contenedor no está "healthy"?
docker compose logs backend --tail 100     # excepciones, stack traces
docker compose logs mysql --tail 50        # ¿MySQL rechazando conexiones?
df -h                                      # ¿disco lleno? (ver EspacioEnDiscoBajo)
```

Si hay stack de monitoreo levantado (`docker-compose.monitoring.yml`):
Prometheus en `:9090` (targets, alertas activas) - ver qué alerta de
`ops/alerts.yml` está en estado `firing`.

## 3. Qué reiniciar (y en qué orden)

- **Sólo el backend está mal** (memoria, conexión colgada): `docker compose
  restart backend`. Es stateless (etapa 05: JWT, sin sesión de servidor) -
  reiniciarlo no corta sesiones de usuarios activos, sólo requests en vuelo.
- **MySQL no responde**: NO reiniciar MySQL a la ligera un sábado a la tarde
  sin antes confirmar que no está en medio de una migración o de un restore.
  Si hay que hacerlo: `docker compose restart mysql`, esperar a que el
  healthcheck vuelva a "healthy" (`docker compose ps`) antes de tocar nada
  más - el backend reintenta la conexión solo (HikariCP).
- **Caddy no consigue certificado TLS**: revisar `docker compose logs caddy`
  - típicamente DNS no apunta bien todavía, o rate-limit de Let's Encrypt (5
    intentos fallidos/hora por dominio). Esperar, no reintentar en loop.
- **Nada de lo anterior alcanza**: `docker compose down && docker compose up
  -d` (reinicio completo del stack). Último recurso - corta todas las
  conexiones activas por unos segundos.

## 4. Si el problema es la base de datos (datos corruptos/perdidos)

Ver `scripts/backup-db.sh` / `scripts/restore-db.sh`. **Antes de restaurar**:
un restore reemplaza TODA la base - confirmar que no hay una alternativa más
chica (ej. corregir una fila a mano) antes de perder los pedidos/pagos del
día. Restore probado y verificado - ver
`docs/especificacion/20-infra-deploy.md`.

## 5. Escalar

Si nada de esto resuelve en ~15 minutos: es un problema que necesita al
desarrollador del sistema, no más intentos de reinicio. Documentar qué se
probó (para no repetirlo) y escalar.
