#!/usr/bin/env bash
# Restore ONLY to isolated, empty, tmpfs-backed recovery-db. Never touches app db.
set -Eeuo pipefail
umask 077
[[ -f compose.yaml && -f scripts/ops/backup.sh && -f compose.recovery.yaml ]] || {
  echo 'Run W10 scripts from the reviewed GrowLab repository root' >&2
  exit 2
}
[[ $# == 2 && "$1" == "--sandbox" ]] || {
  echo "Usage: $0 --sandbox /path/to/growlab_backup.dump" >&2
  exit 2
}
: "${RESTORE_TEST_PASSWORD:?RESTORE_TEST_PASSWORD required for isolated recovery}"
command -v docker >/dev/null || { echo 'Docker CLI unavailable' >&2; exit 2; }
command -v sha256sum >/dev/null || { echo 'sha256sum unavailable' >&2; exit 2; }
archive="$2"
[[ -f "$archive" && ! -L "$archive" && "$archive" == *.dump ]] || {
  echo 'Refusing non-regular, symlinked or non-.dump archive' >&2
  exit 2
}
[[ -f "$archive.sha256" && ! -L "$archive.sha256" ]] || {
  echo 'A regular .sha256 checksum file is required' >&2
  exit 2
}
folder="$(cd "$(dirname "$archive")" && pwd -P)"
file="$(basename "$archive")"
# Check only the expected file, not arbitrary paths embedded in a checksum manifest.
expected="$(cut -d ' ' -f1 < "$archive.sha256")"
[[ "$expected" =~ ^[0-9a-fA-F]{64}$ ]] || {
  echo 'Invalid SHA-256 manifest' >&2
  exit 2
}
actual="$(sha256sum "$archive" | cut -d ' ' -f1)"
[[ "$expected" == "$actual" ]] || {
  echo 'Checksum mismatch; refusing to restore' >&2
  exit 1
}
compose=(docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery)
# This is deliberately NOT configurable. Never accept an arbitrary host or target db.
empty="$("${compose[@]}" exec -T recovery-db \
  psql -X -At -v ON_ERROR_STOP=1 -U recovery -d growlab_recovery \
  -c "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind IN ('r','p','v','m');")"
[[ "$empty" == "0" ]] || {
  echo 'Recovery sandbox is not empty or unavailable; refusing to overwrite' >&2
  exit 1
}
"${compose[@]}" exec -T recovery-db pg_restore \
  --exit-on-error --single-transaction --no-owner --no-acl \
  -U recovery -d growlab_recovery < "$archive"
echo 'Restore succeeded in isolated recovery-db only. No production data was changed.'
