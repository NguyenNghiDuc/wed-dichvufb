#!/usr/bin/env bash
# Offline-safe staging input verification; NEVER launches or alters containers.
set -Eeuo pipefail
umask 077
[[ $# == 0 ]] || { echo "Usage: $0" >&2; exit 2; }
[[ -f compose.yaml && -f compose.staging.yaml && -f ops/Caddyfile.staging ]] || {
  echo 'Run preflight from the repository root' >&2; exit 2;
}
: "${STAGING_HOST:?Set approved STAGING_HOST (not localhost or an IP)}"
: "${POSTGRES_PASSWORD:?Set non-default synthetic staging PostgreSQL password}"
host="$STAGING_HOST"
[[ "$host" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*\.[a-zA-Z]{2,}$ &&
   "$host" != localhost* &&
   "$host" != *.local &&
   "$host" != *.internal &&
   "$host" != *.example.com ]] || {
  echo 'STAGING_HOST must be an approved DNS hostname, not localhost/IP/demo host' >&2
  exit 2
}
[[ "${#POSTGRES_PASSWORD}" -ge 20 &&
  "$POSTGRES_PASSWORD" != CHANGE_ME* &&
  "$POSTGRES_PASSWORD" != password* ]] || {
  echo 'Choose an independently generated staging DB password (>=20 chars)' >&2
  exit 2
}
if [[ -n "${GROWTH_WEBHOOK_SECRET:-}" && "${#GROWTH_WEBHOOK_SECRET}" -lt 32 ]]; then
  echo 'Configured internal webhook secret must have at least 32 bytes' >&2
  exit 2
fi
command -v docker >/dev/null || { echo 'Docker CLI unavailable' >&2; exit 2; }
# --quiet avoids dumping environment secrets into CI/terminal logs.
docker compose -f compose.yaml -f compose.staging.yaml config --quiet
echo 'PASS: staging Compose interpolation and non-secret configuration checks.'
echo 'NOT deployed: check approved DNS, firewall, TLS, credentials, backups and staging UAT.'
