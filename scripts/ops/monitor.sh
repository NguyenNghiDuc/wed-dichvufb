#!/usr/bin/env bash
# W10 read-only operator probe. Designed for an approved external scheduler.
# No secret bodies, cookies, account data, SQL, or GET querystrings in output.
set -Eeuo pipefail
[[ $# == 4 && ( "$1" == "--local" || "$1" == "--tls" ) &&
   "$2" == "--backup-dir" ]] || {
  echo "Usage: $0 --local|--tls --backup-dir /restricted/path MAX_AGE_HOURS" >&2
  exit 2
}
mode="$1"
directory="$3"
age="$4"
[[ "$directory" == /* && -d "$directory" && "$age" =~ ^[0-9]{1,3}$ ]] || {
  echo 'Invalid backup monitor configuration' >&2
  exit 2
}
command -v curl >/dev/null || { echo 'curl unavailable' >&2; exit 2; }
if [[ "$mode" == "--local" ]]; then
  base="http://127.0.0.1:8080"
else
  : "${STAGING_HOST:?Approved STAGING_HOST required}"
  [[ "$STAGING_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*\.[a-zA-Z]{2,}$ ]] || {
    echo 'Invalid staging host' >&2; exit 2
  }
  base="https://$STAGING_HOST"
fi
for probe in /actuator/health /actuator/health/readiness /actuator/health/liveness; do
  if ! body="$(curl --proto '=http,https' --silent --show-error --fail \
      --connect-timeout 5 --max-time 12 "$base$probe" 2>/dev/null)"; then
    echo "CRITICAL: unavailable health probe $probe" >&2
    exit 1
  fi
  [[ "$body" == *'"status":"UP"'* ]] || {
    echo "CRITICAL: failed health probe $probe" >&2
    exit 1
  }
done
if ! bash scripts/ops/backup_inventory.sh --check "$directory" "$age"; then
  echo 'CRITICAL: backup freshness/integrity failed' >&2
  exit 1
fi
echo 'PASS: health, readiness, liveness and backup inventory. External alerting NOT configured.'
