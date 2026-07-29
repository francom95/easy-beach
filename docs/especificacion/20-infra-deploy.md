# Etapa 20 — Infraestructura, deploy y observabilidad

## 1. Resumen

EasyBeach queda listo para producción de forma operable por un equipo chico:
decisión de hosting documentada, Dockerfiles y `docker-compose.yml`
**verificados con un `docker compose up` real** (no solo escritos), CI
extendido, backups con restore **probado de verdad**, métricas y alertas
**verificadas con una prueba controlada real**, runbook de incidentes, y
documento de escalado estacional.

Dos bugs reales aparecieron recién al correr la infraestructura de verdad
(nunca se habrían visto solo leyendo el código) - ver §3.

## 2. Decisión de hosting

Ver [`ADR-006-hosting-y-deploy.md`](../adr/ADR-006-hosting-y-deploy.md): VPS +
Docker Compose, tres escenarios de costo (Bootstrap ~US$15-25/mes, Temporada
~US$40-60/mes en pico, Con margen de resiliencia ~US$70-100/mes), sin fijar
tipo de cambio ARS/USD.

## 3. Dockerfiles + docker-compose (verificado en vivo)

`backend/Dockerfile`, `web/Dockerfile`, `docker-compose.yml`, `Caddyfile`,
`.env.example`. Verificación real hecha en esta etapa (no solo `docker
build`, un `docker compose up` completo con MySQL real, migraciones de
Flyway reales, y llamadas HTTP reales entre contenedores):

1. `docker build` de ambas imágenes: OK.
2. `docker compose config` con `.env.example` copiado a `.env`: resuelve
   todas las variables correctamente.
3. `docker compose up -d mysql backend web`: **falló** - dos bugs reales:

   - **`JwtKeyProvider` resolvía rutas de filesystem como classpath
     resource.** `DefaultResourceLoader.getResource(location)` de Spring
     trata cualquier location sin scheme reconocido como `ClassPathResource`
     - una ruta absoluta real como `/var/easybeach/jwt-keys/private.pem`
     (exactamente el valor que pone `docker-compose.yml`) se buscaba en el
     classpath del JAR, no en el filesystem, y tiraba
     `FileNotFoundException` en el arranque. Nunca se había probado con una
     ruta absoluta real hasta este `docker compose up` - los tests
     existentes no configuran estas variables (perfil local/dev genera un
     par RSA efímero). **Fix:**
     `backend/src/main/java/com/easybeach/identity/security/JwtKeyProvider.java`
     antepone `file:` a cualquier location sin scheme explícito. Regresión
     cubierta por `JwtKeyProviderTest` (nuevo) con una ruta absoluta real
     generada en un `@TempDir`.
   - **Healthcheck de `web` fallaba siempre.** En la imagen `node:22-alpine`,
     `/etc/hosts` resuelve `localhost` a `::1` (IPv6) antes que a
     `127.0.0.1`, pero el server standalone de Next.js sólo escucha en IPv4
     (`0.0.0.0`) - el `HEALTHCHECK` de `web/Dockerfile` (`wget
     http://localhost:3000/`) fallaba con connection refused aunque el
     server respondiera bien. **Fix:** IP explícita (`127.0.0.1`) en vez de
     `localhost`.

4. Con ambos fixes: `docker compose up -d mysql backend web` → los 3
   contenedores healthy, Flyway aplicó las 9 migraciones contra MySQL real,
   `web` alcanzó `backend` por DNS interno de Compose
   (`http://backend:8080/actuator/health` → `{"status":"UP"}`).

## 4. CI/CD

- `.github/workflows/web-ci.yml` (nuevo): lint + `tsc --noEmit` + build.
  Verificado localmente antes de commitear (`npm run lint`, `npx tsc
  --noEmit`, y el build ya se había probado dentro del `docker build`
  de la sección anterior) - las 3 pasan.
- `.github/workflows/deploy.yml` (nuevo): disparo manual (`workflow_dispatch`)
  únicamente - no hay VPS real todavía, así que nadie debería poder
  correrlo sin querer. SSH + `git pull` + `docker compose build && up -d` en
  el VPS (no build-and-push a un registry: ADR-006 elige minimalismo para un
  monolito único). Requiere que un humano configure GitHub Environments
  ("staging"/"production" con required reviewers) y los secrets
  `DEPLOY_HOST`/`DEPLOY_USER`/`DEPLOY_SSH_KEY`/`DEPLOY_PATH` - documentado en
  comentarios del propio workflow.
- `docs/deploy/mobile-build-y-publicacion.md` (nuevo): proceso real de build
  firmado + publicación en Google Play / App Store. **No automatizado**:
  requiere una cuenta de Google Play Console (pago único US$25) y una de
  Apple Developer Program (US$99/año) - ambas necesitan una identidad y un
  medio de pago reales que sólo un humano puede dar de alta. Es una sola app
  blanco-etiquetada (ADR-005), no una por balneario.

## 5. Base de datos: backups (restore probado de verdad)

`scripts/backup-db.sh` / `scripts/restore-db.sh` (mysqldump vía `docker
compose exec`, gzip). Verificación real en esta etapa - no un backup sin
probar:

1. Insertado un `balneario` marcador vía SQL directo contra el MySQL real del
   stack.
2. `./scripts/backup-db.sh` → dump generado, confirmado que contiene el
   marcador (`zcat ... | grep`).
3. Base mutada a propósito: marcador borrado, fila nueva insertada.
4. `./scripts/restore-db.sh <dump>` → confirmado que el marcador volvió y la
   fila post-backup desapareció. **Restore verificado, no asumido.**

Migraciones de Flyway en deploy: `ddl-auto: validate` (nunca genera schema
solo) + Flyway corre al boot del backend contenedor - mismo camino en
dev/staging/prod, sin paso manual aparte.

## 6. Observabilidad

- `/actuator/prometheus` expuesto (Micrometer + `micrometer-registry-prometheus`
  en `backend/pom.xml`), pero **no público**: `Caddyfile` responde 404 a
  `/actuator/*` en el dominio público; sólo alcanzable dentro de la red
  interna de Compose. `SecurityConfig` permite el endpoint sin JWT (un
  scraper de Prometheus no tiene token) apoyado en ese bloqueo de Caddy.
- `ops/alerts.yml`: 4 alertas mínimas (`BackendCaido`, `TasaDeErroresAlta`,
  `LatenciaMenuPublicoAlta`, `EspacioEnDiscoBajo`) - ver `ops/README.md` para
  el detalle de cada umbral y por qué la latencia usa promedio
  (`sum/count`) y no un percentil (`http_server_requests_seconds` es un
  `summary` de Micrometer, no un histograma, salvo que se pague la
  cardinalidad extra de habilitarlo).
- **"Las alertas mínimas dispararon en una prueba controlada" (criterio de
  aceptación) - cumplido con `promtool test rules ops/alerts_test.yml`**:
  unit test con series sintéticas que cruzan cada umbral, sin necesidad de un
  Prometheus corriendo. Resultado: `SUCCESS` (las 4 alertas dispararon en el
  tiempo esperado con las labels/anotaciones correctas).
- `docker-compose.monitoring.yml` (overlay opcional, Prometheus +
  Alertmanager) - probado en vivo: levantado junto al backend real, target
  `easybeach-backend` con `health: up` y sin errores de scrape, las 4 reglas
  cargadas con `health: ok`.
- `docs/deploy/runbook-incidentes.md`: 1 página, qué mirar y qué reiniciar
  (escenario: sábado de enero, 15:00).
- Uptime check externo: **no automatizable** (requiere una cuenta real, ej.
  UptimeRobot) - pasos documentados en `ops/README.md` para que un humano lo
  dé de alta.

## 7. Escalado estacional

Ver [`20-escalado-estacional.md`](20-escalado-estacional.md): checklist de
qué subir antes de diciembre (resize de VPS, pool de conexiones, repetir la
prueba de carga de etapa 19, levantar monitoreo) y qué bajar en abril
(downgrade de VPS, archivar backups, revisar costo real vs. los supuestos de
etapa 02).

## 8. Qué requiere acción humana (no automatizable desde acá)

- Crear y pagar la VM real (VPS), el dominio, y apuntar DNS.
- Cuentas de Google Play Console y Apple Developer Program (identidad y pago
  reales).
- Completar `.env` real en el VPS a partir de `.env.example` (credenciales de
  Mercado Pago, claves JWT/TOKEN_ENCRYPTION_KEY generadas para ese entorno,
  nunca las de este documento).
- Configurar GitHub Environments + secrets para `deploy.yml`.
- Dar de alta el uptime check externo y (si se quiere alertar por email/Slack
  de verdad) completar `ops/alertmanager.yml` con credenciales reales.
- El primer deploy real y el primer submit a las stores (siempre requieren
  intervención humana, incluso con todo lo anterior automatizado).

## 9. Estado final

Suite completa del backend: **151/151 tests, BUILD SUCCESS** (150 de etapa 19
+ `JwtKeyProviderTest` nuevo). `web`: lint + `tsc --noEmit` + build, sin
errores.
