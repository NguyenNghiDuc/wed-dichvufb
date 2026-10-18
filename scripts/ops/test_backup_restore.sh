#!/usr/bin/env bash
# Destructive operations restricted to disposable tmpfs databases. Requires local Docker.
set -Eeuo pipefail
umask 077
[[ $# == 0 ]] || { echo "Usage: $0" >&2; exit 2; }
command -v docker >/dev/null || { echo 'Docker CLI required for end-to-end recovery test' >&2; exit 2; }
command -v od >/dev/null || { echo 'od required to create a random test password' >&2; exit 2; }
[[ -f compose.recovery.yaml && -f compose.yaml ]] || {
  echo 'Run from the repository root' >&2; exit 2;
}
export RESTORE_TEST_PASSWORD
RESTORE_TEST_PASSWORD="$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')"
# Compose parses the base db service too; use only synthetic secrets for this run.
export POSTGRES_PASSWORD="$RESTORE_TEST_PASSWORD"
# Per-run Compose project prevents collisions with existing app/staging containers.
export COMPOSE_PROJECT_NAME="w10recovery$(od -An -N6 -tx1 /dev/urandom | tr -d ' \n')"
scratch="$(mktemp -d)"
compose=(docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery)
cleanup() {
  "${compose[@]}" stop fixture-db recovery-db >/dev/null 2>&1 || true
  "${compose[@]}" rm -sf fixture-db recovery-db >/dev/null 2>&1 || true
  rm -rf -- "$scratch"
}
trap cleanup EXIT
# The only started services use tmpfs and have no published ports or app dependencies.
"${compose[@]}" up -d --wait --wait-timeout 120 fixture-db recovery-db
"${compose[@]}" exec -T fixture-db \
  psql -X -v ON_ERROR_STOP=1 -U fixture -d growlab_fixture <<'SQL'
CREATE TABLE w10_fixture_test (id integer PRIMARY KEY, label text NOT NULL);
INSERT INTO w10_fixture_test (id,label) VALUES (1,'synthetic'),(2,'test-only'),(3,'no-personal-data');
SQL
BACKUP_DIR="$scratch" bash scripts/ops/backup.sh --fixture
archive="$(find "$scratch" -maxdepth 1 -type f -name 'growlab_*.dump' -print -quit)"
[[ -n "$archive" && -f "$archive.sha256" ]] || { echo 'Backup output missing' >&2; exit 1; }
[[ "$(stat -c '%a' "$archive")" == 600 ]] || { echo 'Backup permissions too broad' >&2; exit 1; }
# Deliberately damage a COPY; checksum must reject BEFORE restore is attempted.
corrupt="$scratch/growlab_$(date -u +%Y%m%dT%H%M%SZ)_corrupt.dump"
cp -- "$archive" "$corrupt"
cp -- "$archive.sha256" "$corrupt.sha256"
printf '%s' 'tampered' >> "$corrupt"
if bash scripts/ops/restore_sandbox.sh --sandbox "$corrupt"; then
  echo 'FAIL: tampered archive was accepted' >&2
  exit 1
fi
bash scripts/ops/restore_sandbox.sh --sandbox "$archive"
count="$("${compose[@]}" exec -T recovery-db psql -X -At -v ON_ERROR_STOP=1 \
    -U recovery -d growlab_recovery -c 'SELECT count(*) FROM w10_fixture_test;')"
sum="$("${compose[@]}" exec -T recovery-db psql -X -At -v ON_ERROR_STOP=1 \
    -U recovery -d growlab_recovery -c 'SELECT sum(id) FROM w10_fixture_test;')"
[[ "$count" == 3 && "$sum" == 6 ]] || {
  echo "FAIL: fixture count/sum did not survive round trip ($count/$sum)" >&2
  exit 1
}
# Explicit overwrite protection: a second restore into populated DB must fail.
if bash scripts/ops/restore_sandbox.sh --sandbox "$archive"; then
  echo 'FAIL: second restore into nonempty recovery DB was accepted' >&2
  exit 1
fi
echo 'PASS: synthetic fixture backup, checksum rejection, isolated restore, data verification, overwrite protection.'
