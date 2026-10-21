#!/bin/sh
# Checks an ACTUAL deployed HTTPS staging site. Does not make payment transactions.
set -eu
if [ "$#" -ne 1 ]; then
  echo 'Usage: sh scripts/render-staging-smoke.sh https://YOUR-REAL-RENDER-HOSTNAME' >&2
  exit 2
fi
ORIGIN="${1%/}"
case "$ORIGIN" in
  https://*/*|https://*) ;;
  *) echo 'HTTPS origin is required' >&2; exit 2 ;;
esac
case "$ORIGIN" in
  *'@'*|*'?'*|*'#'*|*' '*)
    echo 'Provide only the HTTPS origin, without userinfo or query' >&2; exit 2 ;;
esac

STATUS_FILE="$(mktemp)"
trap 'rm -f "$STATUS_FILE"' EXIT HUP INT TERM
code() {
  curl --silent --show-error --max-time 20 --connect-timeout 8 \
    -o /dev/null -w '%{http_code}' "$@"
}
curl --silent --show-error --fail --max-time 20 --connect-timeout 8 \
  "$ORIGIN/actuator/health" > "$STATUS_FILE"
if ! grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"' "$STATUS_FILE"; then
  echo 'App health did not return UP' >&2
  exit 1
fi
[ "$(code "$ORIGIN/login")" = "200" ] || { echo 'Login page failed' >&2; exit 1; }
auth_code="$(code "$ORIGIN/wallet")"
case "$auth_code" in
  302|303) ;;
  *) echo "Anonymous wallet must redirect; status was $auth_code" >&2; exit 1 ;;
esac
webhook_code="$(code -X POST -H 'Content-Type: application/json' \
  --data '{}' "$ORIGIN/api/v1/payments/webhooks/casso-v2")"
[ "$webhook_code" = "503" ] || {
  echo "Webhook must remain DISABLED in staging; got HTTP $webhook_code" >&2
  exit 1
}
printf 'HTTPS staging smoke checks PASS; Casso webhooks remain OFF\n'
