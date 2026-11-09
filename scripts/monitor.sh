#!/bin/sh
# VM health check for baby-steps, run from cron every 5 min. Alerts via ntfy once per problem, and once on recovery.
# Topic is read from /root/baby-steps/.ntfy-topic (root-only, never committed).
DISK_MAX=80; MEM_MIN_PCT=20; SWAP_MAX=80; BACKUP_MAX_H=26
CONTAINERS="baby-steps-app baby-steps-postgres-1 edge-caddy"
TOPIC_FILE=/root/baby-steps/.ntfy-topic
STATE=/var/tmp/baby-monitor; mkdir -p "$STATE"
TOPIC=$(cat "$TOPIC_FILE") || exit 1

notify() { # title priority tags message
  curl -s -m 15 -H "Title: $1" -H "Priority: $2" -H "Tags: $3" -d "$4" "https://ntfy.sh/$TOPIC" >/dev/null
}
check() { # key ok(0/1) problem-message ok-message
  key=$1; f="$STATE/$key"
  if [ "$2" = 0 ]; then
    [ -e "$f" ] && { rm -f "$f"; notify "baby-steps OK: $key" default white_check_mark "$4"; }
  else
    [ -e "$f" ] || { : > "$f"; notify "baby-steps ALERT: $key" high warning "$3"; }
  fi
}

disk=$(df / | awk 'NR==2{gsub("%","",$5); print $5}')
check disk $([ "$disk" -ge $DISK_MAX ] && echo 1 || echo 0) "Disk is ${disk}% full (limit ${DISK_MAX}%)" "Disk back to ${disk}%"

mt=$(awk '/MemTotal/{print $2}' /proc/meminfo)
ma=$(awk '/MemAvailable/{print $2}' /proc/meminfo)
mp=$((ma*100/mt))
check memory $([ "$mp" -lt $MEM_MIN_PCT ] && echo 1 || echo 0) "Only ${mp}% memory available (limit ${MEM_MIN_PCT}%)" "Memory back to ${mp}% available"

st=$(awk '/SwapTotal/{t=$2}/SwapFree/{f=$2}END{if(t>0)print int((t-f)*100/t); else print 0}' /proc/meminfo)
check swap $([ "$st" -ge $SWAP_MAX ] && echo 1 || echo 0) "Swap is ${st}% used (limit ${SWAP_MAX}%)" "Swap back to ${st}%"

for c in $CONTAINERS; do
  running=$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)
  check "container-$c" $([ "$running" = true ] && echo 0 || echo 1) "Container $c is not running" "Container $c is running again"
done

newest=$(find /root/baby-steps/backups -name 'babies-*.sql.gz' -mmin -$((BACKUP_MAX_H*60)) 2>/dev/null | head -1)
check backup $([ -n "$newest" ] && echo 0 || echo 1) "No backup newer than ${BACKUP_MAX_H}h" "Backups are fresh again"
exit 0
