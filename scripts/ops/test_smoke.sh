#!/usr/bin/env bash
# Mock health/login for staging smoke safety, no real HTTP traffic.
set -Eeuo pipefail
umask 077
[[ -f scripts/ops/smoke.sh ]] || { echo 'Run from repo root' >&2; exit 2; }
scratch="$(mktemp -d)"
trap 'rm -rf -- "$scratch"' EXIT
mkdir "$scratch/bin"
cat > "$scratch/bin/curl" <<'MOCK'
#!/usr/bin/env bash
for opt in "$@"; do
  [[ "$opt" != '-k' && "$opt" != '--insecure' ]] || exit 99
done
last="${@: -1}"
case "$last" in
  http://127.0.0.1:8080/actuator/health|https://staging.test-owner.test/actuator/health)
    if [[ "${W10_UNHEALTHY:-}" == yes ]]; then echo '{"status":"DOWN"}'; else echo '{"status":"UP"}'; fi
    ;;
  http://127.0.0.1:8080/login|https://staging.test-owner.test/login)
    echo "${W10_LOGIN_CODE:-200}"
    ;;
  *) exit 99 ;;
esac
MOCK
chmod 700 "$scratch/bin/curl"
export PATH="$scratch/bin:$PATH"
export STAGING_HOST=staging.test-owner.test
bash scripts/ops/smoke.sh --local >"$scratch/result"
grep -q '^PASS:' "$scratch/result"
bash scripts/ops/smoke.sh --tls >"$scratch/result"
grep -q 'trusted TLS' "$scratch/result"
if W10_UNHEALTHY=yes bash scripts/ops/smoke.sh --local >"$scratch/result" 2>"$scratch/err"; then
  echo 'FAIL: unhealthy probe incorrectly passed' >&2; exit 1
fi
if W10_LOGIN_CODE=403 bash scripts/ops/smoke.sh --tls >"$scratch/result" 2>"$scratch/err"; then
  echo 'FAIL: login HTTP 403 incorrectly passed' >&2; exit 1
fi
if env -u STAGING_HOST bash scripts/ops/smoke.sh --tls >"$scratch/result" 2>"$scratch/err"; then
  echo 'FAIL: missing TLS hostname passed' >&2; exit 1
fi
echo 'PASS: mocked local/TLS smoke, unhealthy service, 403 and missing host.'
