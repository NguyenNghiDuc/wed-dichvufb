#!/usr/bin/env bash
# Synthetic preflight test. Verifies commands and secrecy without Docker or DNS.
set -Eeuo pipefail
umask 077
[[ -f compose.staging.yaml ]] || { echo 'Run from repository root' >&2; exit 2; }
testdir="$(mktemp -d)"
trap 'rm -rf -- "$testdir"' EXIT
mkdir "$testdir/bin"
cat > "$testdir/bin/docker" <<'SCRIPT'
#!/usr/bin/env bash
[[ "$*" == 'compose -f compose.yaml -f compose.staging.yaml config --quiet' ]] || {
  echo 'Unexpected or unsafe Docker command' >&2
  exit 99
}
printf 'offline-config-checked\n' >> "$W10_PREFLIGHT_MOCK"
SCRIPT
chmod 700 "$testdir/bin/docker"
export PATH="$testdir/bin:$PATH"
export W10_PREFLIGHT_MOCK="$testdir/docker-called"
export STAGING_HOST="staging.test-owner.test"
export POSTGRES_PASSWORD="W10SYNTHETIC_NOT_A_REAL_PASSWORD_2026"
export GROWTH_WEBHOOK_SECRET="W10SYNTHETIC_NOT_A_REAL_WEBHOOK_SECRET_2026"
bash scripts/ops/preflight_staging.sh > "$testdir/out" 2>"$testdir/err"
[[ "$(wc -l < "$W10_PREFLIGHT_MOCK")" -eq 1 ]] || exit 1
! grep -q 'W10SYNTHETIC_NOT' "$testdir/out" "$testdir/err"
! grep -q 'password' "$testdir/out" "$testdir/err"
before="$(wc -l < "$W10_PREFLIGHT_MOCK")"
if STAGING_HOST=localhost bash scripts/ops/preflight_staging.sh > "$testdir/out" 2>"$testdir/err"; then
  echo 'FAIL: localhost staging hostname was accepted' >&2; exit 1
fi
if POSTGRES_PASSWORD=short bash scripts/ops/preflight_staging.sh > "$testdir/out" 2>"$testdir/err"; then
  echo 'FAIL: short staging credential was accepted' >&2; exit 1
fi
if GROWTH_WEBHOOK_SECRET=short bash scripts/ops/preflight_staging.sh > "$testdir/out" 2>"$testdir/err"; then
  echo 'FAIL: short signed-webhook secret was accepted' >&2; exit 1
fi
[[ "$(wc -l < "$W10_PREFLIGHT_MOCK")" -eq "$before" ]] || {
  echo 'FAIL: an unsafe preflight invoked Docker' >&2; exit 1
}
echo 'PASS: synthetic staging config uses Docker config --quiet and rejects unsafe values.'
