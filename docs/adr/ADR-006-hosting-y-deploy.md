# ADR-006 — Hosting y deploy: VPS + Docker Compose

- **Estado:** Aceptada
- **Fecha:** 2026-07-29
- **Etapa:** 20

## Contexto

Hay que elegir dónde y cómo corre EasyBeach en producción. Restricciones que
importan más que en una decisión de hosting genérica:

- **Equipo chico**: nadie va a operar Kubernetes, service mesh, ni una
  cuenta cloud con 40 servicios distintos. La operación tiene que caber en la
  cabeza de una persona.
- **Negocio estacional** (ADR-001/ADR-002, supuestos de etapa 02): pico real
  solo en diciembre–marzo (~5.000–15.000 clientes concurrentes, 100–200
  pedidos/min, **toda la plataforma**); el resto del año la carga es mínima.
  Pagar todo el año lo que hace falta en enero es tirar plata.
- **Monolito modular, una sola base MySQL compartida** (ADR-001/ADR-002): no
  hay nada que se beneficie de auto-scaling horizontal fino por servicio —
  hay UN deployable y UNA base.
- **Sin presupuesto mensual objetivo definido** (input faltante de esta
  etapa) → se proponen tres escenarios de costo en vez de una única cifra.

## Opciones evaluadas

**(a) VPS + Docker Compose.** Un servidor virtual (o dos: uno para
staging/uno para prod, o ambos entornos en el mismo VPS con puertos/redes
Docker separadas si el presupuesto es ajustado), corriendo el mismo
`docker-compose.yml` que ya se usa en desarrollo (backend + MySQL + web +
proxy reverso con TLS automático).

- ✅ Costo predecible y bajo; se paga por VM, no por request/GB con sorpresas.
- ✅ Mismo artefacto de deploy que local (Docker Compose) → "funciona en mi
  máquina" y "funciona en prod" son literalmente el mismo comando.
- ✅ Control total: escalar la VM verticalmente antes de diciembre y bajarla
  en abril es un resize, no una migración.
- ❌ Los backups, parches de SO y el reinicio ante una caída son responsabilidad
  del equipo (no hay un botón "restore" de un proveedor gestionado) → mitigado
  con el script de backup/restore y el runbook de esta etapa.
- ❌ Un solo punto de falla si se usa una sola VM → aceptable para el MVP
  (el propio ADR-002 ya asume esto), con un plan de escalado documentado
  (ver `docs/especificacion/20-escalado-estacional.md`) para cuando deje de
  serlo.

**(b) PaaS (Railway, Render, Fly.io).** Git push → build → deploy gestionado.

- ✅ Cero administración de SO; certificados TLS y rollbacks automáticos.
- ❌ La base de datos gestionada de estos proveedores cuesta 2–4x un VPS
  equivalente para el mismo RAM/CPU, y escalar "hacia abajo" en abril suele
  ser más manual de lo que promete el marketing.
- ❌ Menos control sobre el proceso de backup/restore real (criterio de
  aceptación de esta etapa exige *probarlo*, no solo confiar en el proveedor).
- Descartado para el MVP por costo; queda como opción real de evolución si el
  equipo crece y el tiempo de un dev vale más que la diferencia de precio.

**(c) Cloud administrado (AWS ECS/Fargate + RDS, GCP Cloud Run + Cloud SQL).**

- ✅ El más escalable de los tres a largo plazo; auto-scaling real, multi-AZ.
- ❌ Todo el costo/complejidad de una arquitectura distribuida (IAM, VPC,
  service discovery, logging centralizado propio) para una app que es UN
  monolito y UNA base — exactamente el mismo argumento por el que ADR-002
  descartó microservicios. Sobredimensionado para el MVP.
- Descartado por ahora; el monolito en contenedores es portable a esto sin
  reescribir nada si algún día hace falta (mismo Dockerfile, distinto
  orquestador).

## Decisión

**VPS + Docker Compose**, con TLS automático vía proxy reverso (Caddy) y
push-to-deploy manual-gatillado desde CI (ver `docs/especificacion/20-infra-deploy.md`
para el pipeline). MySQL corre en el mismo Compose stack (no gestionado) —
compensado con el script de backup/restore *probado* (no solo escrito) de
esta etapa.

Tres escenarios de costo (en USD, que es como facturan estos proveedores —
el equivalente en ARS depende del tipo de cambio del momento, no se fija un
valor acá):

| Escenario | Infra | Costo aprox./mes (USD) | Cuándo usarlo |
|---|---|---|---|
| **Bootstrap** | 1 VPS (4GB RAM / 2 vCPU, ej. Hetzner CX22 / DO Basic Droplet) + backups a object storage barato (Backblaze B2, ~centavos/GB) | **~US$15–25** | Pre-lanzamiento y temporada baja (abril–noviembre) |
| **Temporada** | 1 VPS más grande (8GB RAM / 4 vCPU) durante dic–mar, downgrade el resto del año | **~US$40–60** en temporada, ~US$15–25 el resto | Recomendado — alineado 1:1 con la estacionalidad del negocio (ver doc de escalado) |
| **Con margen de resiliencia** | 2 VPS (activo + réplica fría o segundo entorno) + object storage + dominio + monitor externo | **~US$70–100** | Si un balneario grande empieza a depender de esto para vender y una caída de horas es inaceptable |

Costos no cubiertos en la tabla (similares en las tres): dominio
(~US$10–15/año), certificado TLS (gratis vía Let's Encrypt/Caddy), cuenta de
Apple Developer (US$99/año, obligatoria para publicar en App Store), cuenta
de Google Play (pago único US$25).

**Recomendación:** empezar en "Bootstrap", pasar a "Temporada" antes de
diciembre (ver documento de escalado estacional), reevaluar "Con margen de
resiliencia" solo si el volumen real de un balneario en producción lo
justifica.

## Consecuencias

- El deploy real requiere que alguien **cree y pague** la VM, el dominio y
  las cuentas de store — ninguna de esas acciones es algo que se pueda
  automatizar sin decisiones humanas de presupuesto y titularidad de cuenta;
  esta etapa deja el Dockerfile/Compose/CI listos para que ese "primer
  deploy" sea un checklist corto, no un proyecto.
- Migrar de "Bootstrap" a un cloud administrado más adelante no exige
  reescribir la app: el mismo Dockerfile del backend corre en ECS/Cloud Run
  sin cambios; lo único no portable es el `docker-compose.yml` de
  orquestación, que ya está pensado como la pieza más descartable.
- El único estado que no vive en la base de datos es la lista de conexiones
  SSE en memoria (ADR-003) — un restart del backend (deploy, reinicio del
  VPS) corta esas conexiones; el fallback de polling ya cubre esto por
  diseño, no es un caso nuevo a resolver acá.
