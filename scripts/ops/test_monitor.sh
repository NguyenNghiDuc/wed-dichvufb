#!/usr/bin/env bash
# W10 fully local regression for health monitoring, no Docker/network/real backup.
set -Eeuo pipefail
umask 077
[[ -f scripts/ops/monitor.sh ]] || { echo 'Run from repo root' >&2; exit 2; }
sandbox="$(mktemp -d)"
trap 'rm -rf -- "$sandbox"' EXIT
mkdir -p "$sandbox/bin" "$sandbox/backups"
cat > "$sandbox/bin/curl" <<'MOCK'
#!/usr/bin/env bash
last="${@: -1}"
case "$last" in
  http://127.0.0.1:8080/actuator/health|http://127.0.0.1:8080/actuator/health/readiness|http://127.0.0.1:8080/actuator/health/liveness)
    if [[ -n "${MOCK_UNHEALTHY:-}" && "$last" == *"$MOCK_UNHEALTHY" ]]; then
      echo '{"status":"DOWN"}'
    else
      echo '{"status":"UP"}'
    fi
    ;;
  *) exit 99 ;;
esac
MOCK
chmod 700 "$sandbox/bin/curl"
export PATH="$sandbox/bin:$PATH"
file="growlab_$(date -u +%Y%m%dT%H%M%SZ)_synthetic.dump"
printf 'isolated test only' > "$sandbox/backups/$file"
(cd "$sandbox/backups" && sha256sum "$file" > "$file.sha256")
bash scripts/ops/monitor.sh --local --backup-dir "$sandbox/backups" 24 > "$sandbox/output"
grep -q 'PASS: health, readiness, liveness' "$sandbox/output"
export MOCK_UNHEALTHY=readiness
if bash scripts/ops/monitor.sh --local --backup-dir "$sandbox/backups" 24 >"$sandbox/stdout" 2>"$sandbox/stderr"; then
  echo 'FAIL: unhealthy readiness was accepted' >&2; exit 1
fi
unset MOCK_UNHEALTHY
printf 'tampered' >> "$sandbox/backups/$file"
if bash scripts/ops/monitor.sh --local --backup-dir "$sandbox/backups" 24 >"$sandbox/stdout" 2>"$sandbox/stderr"; then
  echo 'FAIL: corrupted backup was accepted' >&2; exit 1
fi
if bash scripts/ops/monitor.sh --tls --backup-dir "$sandbox/backups" 24 >"$sandbox/stdout" 2>"$sandbox/stderr"; then
  echo 'FAIL: TLS probe without approved host was accepted' >&2; exit 1
fi
echo 'PASS: W10 monitoring accepts healthy fixture, rejects DOWN probe/tampered archive/missing TLS host.'
