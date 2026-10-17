#!/usr/bin/env bash
# Check that the Codespaces port serves the patched GrowLab login page.
# This script never kills processes, changes credentials, or modifies the database.
set -euo pipefail

PORT="${PORT:-8000}"
if [[ ! "$PORT" =~ ^[0-9]+$ ]] || (( PORT < 1 || PORT > 65535 )); then
  echo "Invalid PORT: $PORT" >&2
  exit 2
fi

BASE="http://127.0.0.1:${PORT}"
MODE="${1:-check}"
REVISION="session-recovery-v2"

case "$MODE" in
  check)
    workdir="$(mktemp -d)"
    trap 'rm -rf "$workdir"' EXIT

    if ! status="$(curl --noproxy '*' --silent --show-error --connect-timeout 3 \
      --max-time 10 --dump-header "$workdir/headers" \
      --output "$workdir/body" --write-out '%{http_code}' "$BASE/login")"; then
      echo "Cannot reach GrowLab at $BASE/login." >&2
      echo "Check that Spring Boot is running and your Codespaces forwarded port matches PORT=$PORT." >&2
      exit 1
    fi

    revision="$(awk 'BEGIN{IGNORECASE=1} tolower($1)=="x-growlab-auth-revision:" {gsub("\\r","",$2); print $2}' "$workdir/headers" | head -n1)"
    echo "Local /login HTTP status: $status"
    echo "GrowLab revision: ${revision:-missing}"
    if [[ "$status" != "200" || "$revision" != "$REVISION" ]]; then
      echo "This port is not serving the verified login fix." >&2
      echo "Stop the OLD Java process on port $PORT, then run: bash scripts/codespaces-login.sh run" >&2
      echo "If local check passes but the forwarded URL still returns 403, inspect Codespaces port visibility and sign-in." >&2
      exit 1
    fi
    echo "PASS: patched GrowLab /login is available locally at $BASE/login."
    ;;
  run)
    if curl --noproxy '*' --silent --connect-timeout 1 --max-time 2 \
      "$BASE/" >/dev/null 2>&1; then
      echo "Port $PORT already answers HTTP. Stop the existing process first; not replacing it automatically." >&2
      exit 1
    fi
    echo "Starting GrowLab on port $PORT. In a second terminal, run:"
    echo "  bash scripts/codespaces-login.sh check"
    exec mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=$PORT"
    ;;
  *)
    echo "Usage: PORT=8000 bash scripts/codespaces-login.sh [check|run]" >&2
    exit 2
    ;;
esac
