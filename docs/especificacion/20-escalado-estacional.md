# Escalado estacional

EasyBeach es un negocio de temporada (ADR-006, supuestos de etapa 02): el
pico real es diciembre–marzo (~5.000–15.000 clientes concurrentes,
100–200 pedidos/min **en toda la plataforma**), y el resto del año la carga
es mínima. Este documento es la checklist de qué subir antes de diciembre y
qué bajar en abril - no un rediseño, un ajuste de perilla sobre la misma
infra (ADR-006: "un resize, no una migración").

## Antes de diciembre (2-3 semanas antes, no el 1° de diciembre)

1. **Resize del VPS**: de "Bootstrap" (4GB RAM / 2 vCPU) a "Temporada" (8GB
   RAM / 4 vCPU) - ver tabla de costos en ADR-006. Un resize de VPS no
   requiere reescribir nada: mismo `docker-compose.yml`.
2. **Pool de conexiones de MySQL**: HikariCP corre con el default de Spring
   Boot (10 conexiones) - no hay override hoy
   (`backend/src/main/resources/application*.yml`). Con más RAM/vCPU en el
   VPS, subir `spring.datasource.hikari.maximum-pool-size` (probar contra la
   carga real antes de fijar un número - la etapa 19 ya corrió una prueba de
   carga con la topología actual, repetirla después de cualquier cambio acá).
3. **Reconfirmar la prueba de carga de etapa 19** contra el VPS de temporada
   real (no solo local) - los supuestos de escala de etapa 02 son el
   objetivo a validar, no un número que se asume y ya.
4. **Levantar el stack de monitoreo** si no está corriendo todo el año
   (`docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d`,
   ver `ops/README.md`) - es en temporada cuando más vale la pena tener
   alertas activas.
5. **Confirmar el uptime check externo** (UptimeRobot u otro, ver
   `ops/README.md`) está dado de alta y notificando a quien esté de guardia.
6. **Revisar espacio en disco** disponible para backups diarios acumulados
   durante 4 meses de temporada (`scripts/backup-db.sh` no rota archivos
   viejos por diseño simple - copiar backups viejos a object storage barato,
   ej. Backblaze B2 per ADR-006, y limpiar el disco local periódicamente).

## Durante la temporada (diciembre-marzo)

- Vigilar las 4 alertas mínimas (`ops/alerts.yml`): `BackendCaido`,
  `TasaDeErroresAlta`, `LatenciaMenuPublicoAlta`, `EspacioEnDiscoBajo`.
- `EspacioEnDiscoBajo` es la que más probablemente dispare por acumulación de
  backups + logs, no por un bug - revisar el runbook
  (`docs/deploy/runbook-incidentes.md`) antes de asumir que es un incidente.
- Si un balneario grande empieza a depender de esto para vender en serio,
  reevaluar el escenario "Con margen de resiliencia" de ADR-006 (2 VPS) en
  vez de esperar a que un solo punto de falla se convierta en un incidente
  real.

## En abril (fin de temporada)

1. **Downgrade del VPS** de vuelta a "Bootstrap" (4GB/2vCPU) - la carga baja
   de forma real y sostenida el resto del año (supuesto de etapa 02).
2. **Revertir** `spring.datasource.hikari.maximum-pool-size` si se subió (más
   conexiones abiertas de las que hacen falta es solo memoria desperdiciada
   en temporada baja).
3. **Archivar** los backups de la temporada a almacenamiento frío y liberar
   espacio en el VPS.
4. **Bajar el stack de monitoreo** si se decidió no correrlo todo el año
   (`docker compose -f docker-compose.yml -f docker-compose.monitoring.yml down`)
   - el uptime check externo sigue siendo barato/gratuito, mantenerlo activo
   igual.
5. **Revisar el costo real del mes de enero/febrero** contra los escenarios
   de ADR-006 y ajustar la recomendación para la temporada siguiente si el
   supuesto de escala de etapa 02 resultó desactualizado (más o menos
   balnearios/clientes de los proyectados).
