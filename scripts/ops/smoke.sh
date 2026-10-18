#!/usr/bin/env bash
# Read-only health smoke. No DB writes, login attempts or deployment actions.
set -Eeuo pipefail
[[ $# == 1 && ( "$1" == "--local" || "$1" == "--tls" ) ]] || {
  echo "Usage: $0 --local | --tls" >&2
  exit 2
}
command -v curl >/dev/null || { echo 'curl required' >&2; exit 2; }
if [[ "$1" == "--local" ]]; then
  base="http://127.0.0.1:8080"
else
  : "${STAGING_HOST:?Provide approved STAGING_HOST for HTTPS smoke}"
  [[ "$STAGING_HOST" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]*\.[a-zA-Z]{2,}$ ]] || {
    echo 'Invalid staging hostname' >&2; exit 2;
  }
  base="https://$STAGING_HOST"
fi
health="$(curl --proto '=http,https' --fail --silent --show-error \
    --connect-timeout 5 --max-time 15 "$base/actuator/health")"
[[ "$health" == *'"status":"UP"'* ]] || {
  echo 'Health endpoint did not return UP' >&2; exit 1;
}
# GET, not POST: does not create accounts, orders or sessions.
code="$(curl --proto '=http,https' --silent --show-error --output /dev/null \
    --write-out '%{http_code}' --connect-timeout 5 --max-time 15 "$base/login")"
[[ "$code" == 200 ]] || {
  echo "GET /login unexpected HTTP $code" >&2; exit 1;
}
if [[ "$1" == "--tls" ]]; then
  [[ "$base" == https://* ]] || exit 1
  echo 'PASS: trusted TLS, health UP, public login HTTP 200 (not an authorization UAT).'
else
  echo 'PASS: local loopback health UP and login HTTP 200 (not a production test).'
fi
