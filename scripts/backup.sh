#!/bin/sh
# Nightly Postgres dump for baby-steps (runs on the VM from cron). Keeps 14 days.
set -eu
DIR=/root/baby-steps/backups
mkdir -p "$DIR"
OUT="$DIR/babies-$(date +%F-%H%M).sql.gz"
docker exec baby-steps-postgres-1 pg_dump -U babies -d babies --no-owner | gzip > "$OUT.tmp"
gzip -t "$OUT.tmp"
mv "$OUT.tmp" "$OUT"
find "$DIR" -name 'babies-*.sql.gz' -mtime +14 -delete
echo "backup ok: $OUT"
