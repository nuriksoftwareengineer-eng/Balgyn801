#!/bin/sh
# Balgyn — PostgreSQL daily backup script
# Runs inside the db container or on the host with access to the db container.
#
# Usage (from docker compose project root):
#   docker exec balgyn801-main-db-1 /backups/backup-postgres.sh
#
# Environment variables (passed by docker-compose):
#   POSTGRES_USER     — database user (default: postgres)
#   POSTGRES_DB       — database name (default: balgynbol-spring)
#   BACKUP_KEEP_DAYS  — how many daily backups to retain (default: 30)
#
# Output: /backups/daily-YYYY-MM-DD.sql.gz

set -e

POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-balgynbol-spring}"
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-30}"
BACKUP_DIR="/backups"
TIMESTAMP=$(date +%Y-%m-%d)
BACKUP_FILE="${BACKUP_DIR}/daily-${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

echo "[backup] Starting pg_dump of ${POSTGRES_DB} at $(date)"
pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" | gzip > "${BACKUP_FILE}"
echo "[backup] Saved: ${BACKUP_FILE} ($(du -sh "${BACKUP_FILE}" | cut -f1))"

# Rotate — delete backups older than BACKUP_KEEP_DAYS
find "${BACKUP_DIR}" -name "daily-*.sql.gz" -mtime "+${BACKUP_KEEP_DAYS}" -delete
echo "[backup] Rotation complete. Kept last ${BACKUP_KEEP_DAYS} days."
echo "[backup] Done at $(date)"
