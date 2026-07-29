#!/usr/bin/env bash
# Etapa 20 - backup de la base "easybeach" del stack de docker-compose.yml
# (servicio `mysql`, sin puerto publicado - por eso el dump corre DENTRO del
# contenedor via `docker compose exec`, no contra localhost).
#
# Uso: ./scripts/backup-db.sh [directorio-destino]   (default: ./backups)
#
# Pensado para correr desde cron en el VPS, ej.:
#   0 4 * * * cd /opt/easybeach && ./scripts/backup-db.sh >> /var/log/easybeach-backup.log 2>&1
# Copiar los .sql.gz resultantes a almacenamiento fuera del VPS (ej. rsync a
# otro host, o subida a un bucket) es responsabilidad de quien opera el
# servidor real - este script sólo genera el dump local.
set -euo pipefail

cd "$(dirname "$0")/.."

DEST_DIR="${1:-./backups}"
mkdir -p "$DEST_DIR"

TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUT_FILE="$DEST_DIR/easybeach-$TIMESTAMP.sql.gz"

docker compose exec -T mysql sh -c \
  'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers easybeach' \
  | gzip > "$OUT_FILE"

echo "Backup escrito en $OUT_FILE ($(du -h "$OUT_FILE" | cut -f1))"
