# Observabilidad (etapa 20)

## Métricas

El backend expone `/actuator/prometheus` (Micrometer + `micrometer-registry-prometheus`,
ver `backend/pom.xml`). No se publica al dominio público: `Caddyfile` responde
404 a cualquier `/actuator/*` en `{$API_DOMAIN}` - sólo es alcanzable dentro
de la red interna de `docker-compose.yml` (`backend:8080/actuator/prometheus`).

## Stack de monitoreo (opcional)

`docker-compose.monitoring.yml` es un overlay - **no se levanta con el stack
principal por defecto** (ADR-006: minimizar footprint fuera de temporada en
un VPS chico). Levantarlo con:

```bash
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Esto agrega Prometheus (scrapea `backend:8080/actuator/prometheus` cada 15s,
`ops/prometheus.yml`) y Alertmanager (`ops/alertmanager.yml` - el receiver
"default" está vacío, hay que completar `email_configs`/`webhook_configs`
reales antes de depender de esto para alertar de verdad).

## Alertas mínimas (`ops/alerts.yml`)

| Alerta | Condición | Duración |
|---|---|---|
| `BackendCaido` | `up == 0` | 1 min |
| `TasaDeErroresAlta` | >5% de las respuestas son 5xx | 5 min |
| `LatenciaMenuPublicoAlta` | latencia promedio de `GET /api/v1/balnearios/{slug}/menu` > 800ms | 5 min |
| `EspacioEnDiscoBajo` | menos del 15% de espacio libre en disco | 10 min |

`http_server_requests_seconds` es un `Timer` de Micrometer - Spring Boot lo
exporta como `summary` (`_sum`/`_count`/`_max`), no como histograma con
`_bucket`, salvo que se habilite
`management.metrics.distribution.percentiles-histogram.http.server.requests`
(no se hizo: agregar cardinalidad de buckets por URI no vale la pena a esta
escala). Por eso `LatenciaMenuPublicoAlta` usa `rate(_sum)/rate(_count)`
(latencia **promedio**), no un percentil.

### Verificación (prueba controlada real)

`ops/alerts_test.yml` es un unit test de `promtool` con series sintéticas que
cruzan cada umbral - no depende de un Prometheus corriendo. Correrlo:

```bash
docker run --rm --entrypoint promtool -v "$PWD/ops:/ops" prom/prometheus test rules /ops/alerts_test.yml
```

Resultado esperado: `SUCCESS` (las 4 alertas dispararon en la corrida usada
para verificar esta etapa - ver `docs/especificacion/20-infra-deploy.md`).
Volver a correr esto después de cualquier cambio a `alerts.yml`.

## Uptime check externo

No creable desde acá (requiere una cuenta real). Documentado para que un
humano lo dé de alta en [UptimeRobot](https://uptimerobot.com) (tiene plan
gratuito) u otro servicio equivalente:

1. Monitor HTTP(s) contra `https://<WEB_DOMAIN>/` (la home del panel web) -
   confirma DNS + TLS + que Caddy y el contenedor `web` están arriba.
2. Monitor HTTP(s) contra `https://<API_DOMAIN>/api/v1/balnearios/<slug-demo>/menu` -
   confirma que el endpoint más consultado de la plataforma responde de
   punta a punta (Caddy -> backend -> MySQL).
3. Intervalo: 5 minutos (plan gratuito de UptimeRobot). Alertar por
   email/SMS/webhook a quien esté de guardia.
