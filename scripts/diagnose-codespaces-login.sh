#!/usr/bin/env bash
# Read-only checks for a local GrowLab instance behind Codespaces port forwarding.
# Never print cookies, session tokens, passwords or full process command lines.
set -u

port="${1:-8000}"
if ! [[ "${port}" =~ ^[0-9]{1,5}$ ]] || (( port < 1 || port > 65535 )); then
  echo "Usage: bash scripts/diagnose-codespaces-login.sh [port: 1..65535]" >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl is required. Install curl in this environment." >&2
  exit 2
fi

echo "== GrowLab Codespaces /login diagnosis (local only) =="
if command -v git >/dev/null 2>&1 && git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  printf 'Branch: '
  git branch --show-current
  printf 'Commit: '
  git rev-parse --short HEAD
else
  echo "Git: unavailable from this working directory"
fi

echo "Local listener for port ${port}:"
if command -v ss >/dev/null 2>&1; then
  # Shows bind socket and process name/PID only, not CLI arguments or environment.
  ss -ltnp "( sport = :${port} )" 2>/dev/null || true
elif command -v lsof >/dev/null 2>&1; then
  lsof -nP -iTCP:"${port}" -sTCP:LISTEN 2>/dev/null || true
else
  echo "Neither ss nor lsof found; cannot inspect the port owner."
fi

headers="$(mktemp)"
error_log="$(mktemp)"
trap 'rm -f "${headers}" "${error_log}"' EXIT

probe() {
  local label="$1" forwarded="$2" code
  local -a extra=()
  if [[ "${forwarded}" = "yes" ]]; then
    extra=(-H "X-Forwarded-Proto: https" -H "X-Forwarded-Host: example-8000.app.github.dev")
  fi

  : > "${headers}"
  : > "${error_log}"
  if ! code="$(curl --silent --show-error --max-time 8 --max-redirs 0 \
      -D "${headers}" -o /dev/null -w '%{http_code}' \
      "${extra[@]}" "http://127.0.0.1:${port}/login" 2>"${error_log}")"; then
    printf '%s: CONNECTION FAILED\n' "${label}"
    sed -n '1,2p' "${error_log}"
    return 1
  fi
  local revision
  revision="$(grep -i '^X-GrowLab-Auth-Revision:' "${headers}" | tr -d '\r' | tail -n 1 || true)"
  printf '%s: HTTP %s\n' "${label}" "${code}"
  if [[ -n "${revision}" ]]; then printf '%s\n' "${revision}"; else echo "X-GrowLab-Auth-Revision: MISSING"; fi

  if [[ "${code}" != "200" ]] || ! [[ "${revision}" =~ session-recovery-v2 ]]; then
    return 1
  fi
}

local_ok=0
forwarded_ok=0
probe "Direct localhost GET /login" no && local_ok=1
probe "Simulated reverse-proxy GET /login" yes && forwarded_ok=1

if (( local_ok && forwarded_ok )); then
  echo "RESULT: LOCAL APP OK. If the browser URL still shows 403, inspect"
  echo "the Codespaces PORTS panel (8000 forwarded to this Codespace, visibility/access),"
  echo "the exact URL, browser session/cookies, and any proxy authorization."
  exit 0
fi

echo "RESULT: LOCAL APP NOT VERIFIED."
echo "Expected HTTP 200 and X-GrowLab-Auth-Revision: session-recovery-v2 for both probes."
echo "Possible causes: a stale Java process, wrong checkout, wrong port, wrong application,"
echo "or Spring Security error in the running binary."
echo "Don't kill an unknown process. Stop the intended app in its original terminal,"
echo "then restart it from the verified branch."
exit 1
