#!/usr/bin/env bash
# Offline fail-closed tests, no Docker engine or PostgreSQL required.
set -Eeuo pipefail
umask 077
[[ -f scripts/ops/backup.sh && -f scripts/ops/restore_sandbox.sh ]] || {
  echo 'Run from repository root' >&2; exit 2;
}
for script in scripts/ops/*.sh; do bash -n "$script"; done
sandbox="$(mktemp -d)"
cleanup() { rm -rf -- "$sandbox"; }
trap cleanup EXIT
mkdir -p "$sandbox/bin"
cat > "$sandbox/bin/docker" <<'MOCK'
#!/usr/bin/env bash
echo "UNEXPECTED Docker invocation" >> "$W10_MOCK_CALLS"
exit 99
MOCK
chmod 700 "$sandbox/bin/docker"
export W10_MOCK_CALLS="$sandbox/docker_calls"
PATH="$sandbox/bin:$PATH"
export PATH
expect_refused() {
  local name="$1"
  shift
  if "$@" >"$sandbox/stdout" 2>"$sandbox/stderr"; then
    echo "FAIL: $name was accepted" >&2
    exit 1
  fi
  [[ ! -e "$W10_MOCK_CALLS" ]] || {
    echo "FAIL: $name called Docker before passing guardrail" >&2
    exit 1
  }
  echo "PASS: $name refuses before accessing any DB"
}
unset GROWLAB_BACKUP_CONFIRM RESTORE_TEST_PASSWORD || true
expect_refused 'app db without confirmation' bash scripts/ops/backup.sh --db
expect_refused 'fixture without test password' bash scripts/ops/backup.sh --fixture
expect_refused 'staging with no DNS or secret' env -u STAGING_HOST -u POSTGRES_PASSWORD bash scripts/ops/preflight_staging.sh
expect_refused 'invalid rollback SHAs' bash scripts/ops/rollback_plan.sh --plan BAD-SHA ALSO-BAD
expect_refused 'TLS smoke without host' env -u STAGING_HOST bash scripts/ops/smoke.sh --tls
export RESTORE_TEST_PASSWORD='fixture-test-only-not-a-real-secret'
export BACKUP_DIR='./backups'
expect_refused 'relative backup path' bash scripts/ops/backup.sh --fixture
export BACKUP_DIR="$(pwd -P)"
expect_refused 'backup inside repository' bash scripts/ops/backup.sh --fixture
mkdir -p "$sandbox/open"
chmod 755 "$sandbox/open"
export BACKUP_DIR="$sandbox/open"
expect_refused 'world-readable backup folder' bash scripts/ops/backup.sh --fixture
unset BACKUP_DIR

expect_refused 'missing archive' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/missing.dump"
printf 'fake-archive' > "$sandbox/wrong.dump"
printf '%064d  wrong.dump\n' 0 > "$sandbox/wrong.dump.sha256"
expect_refused 'bad checksum' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/wrong.dump"
ln -s "$sandbox/wrong.dump" "$sandbox/link.dump"
expect_refused 'symlinked archive' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/link.dump"
# Read-only inventory detects healthy checksum, then a tampered archive.
mkdir -p "$sandbox/backups"
filename="growlab_$(date -u +%Y%m%dT%H%M%SZ)_synthetic.dump"
printf 'fake-but-syntactically-checksummed-fixture' > "$sandbox/backups/$filename"
(cd "$sandbox/backups" && sha256sum "$filename" > "$filename.sha256")
bash scripts/ops/backup_inventory.sh --check "$sandbox/backups" 24 > "$sandbox/inventory"
grep -q 'PASS:' "$sandbox/inventory"
printf 'tampered' >> "$sandbox/backups/$filename"
expect_refused 'tampered backup inventory' bash scripts/ops/backup_inventory.sh --check "$sandbox/backups" 24
# Old backup copied today must still be classified stale by the archived UTC timestamp.
rm -f -- "$sandbox/backups/$filename" "$sandbox/backups/$filename.sha256"
old="growlab_20200101T000000Z_synthetic.dump"
printf 'old verified fixture archive' > "$sandbox/backups/$old"
(cd "$sandbox/backups" && sha256sum "$old" > "$old.sha256")
expect_refused 'stale copied backup inventory' bash scripts/ops/backup_inventory.sh --check "$sandbox/backups" 24
# A printable rollback plan must not touch Git, Compose or SQL.
bash scripts/ops/rollback_plan.sh --plan aaaaaaa bbbbbbb > "$sandbox/rollback-plan"
grep -q 'NO COMMANDS EXECUTED' "$sandbox/rollback-plan"
[[ ! -e "$W10_MOCK_CALLS" ]] || { echo 'Rollback invoked Docker' >&2; exit 1; }
# Independent test programs keep their own isolated mock commands and no live network.
bash scripts/ops/test_preflight.sh
bash scripts/ops/test_smoke.sh
bash scripts/ops/test_monitor.sh
echo 'PASS: offline Bash syntax and fail-closed operational guardrails'
