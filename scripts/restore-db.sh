#!/usr/bin/env bash
# Etapa 20 - restaura un backup generado por backup-db.sh sobre la base
# "easybeach" del stack de docker-compose.yml. DESTRUCTIVO: reemplaza el
# contenido de la base completa por el del dump.
#
# Uso: ./scripts/restore-db.sh <archivo.sql.gz>
set -euo pipefail

cd "$(dirname "$0")/.."

BACKUP_FILE="${1:?Uso: restore-db.sh <archivo.sql.gz>}"
[ -f "$BACKUP_FILE" ] || { echo "No existe: $BACKUP_FILE" >&2; exit 1; }

echo "Esto reemplaza TODA la base 'easybeach' con el contenido de $BACKUP_FILE."
read -r -p "Escribi 'si' para confirmar: " CONFIRM
[ "$CONFIRM" = "si" ] || { echo "Cancelado."; exit 1; }

gunzip -c "$BACKUP_FILE" | docker compose exec -T mysql sh -c \
  'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" easybeach'

echo "Restore completo."
