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
export RESTORE_TEST_PASSWORD='fixture-test-only-not-a-real-secret'
expect_refused 'missing archive' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/missing.dump"
printf 'fake-archive' > "$sandbox/wrong.dump"
printf '%064d  wrong.dump\n' 0 > "$sandbox/wrong.dump.sha256"
expect_refused 'bad checksum' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/wrong.dump"
ln -s "$sandbox/wrong.dump" "$sandbox/link.dump"
expect_refused 'symlinked archive' bash scripts/ops/restore_sandbox.sh --sandbox "$sandbox/link.dump"
echo 'PASS: offline Bash syntax and fail-closed operational guardrails'
