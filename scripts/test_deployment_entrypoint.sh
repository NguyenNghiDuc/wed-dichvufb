#!/bin/sh
# Safe entrypoint regression test. Never starts a JVM or connects to a bank.
set -eu
SCRIPT="$(dirname "$0")/deployment-entrypoint.sh"
TEST_DIR="$(mktemp -d)"
trap 'rm -rf "$TEST_DIR"' EXIT HUP INT TERM
cat > "$TEST_DIR/java" <<'MOCK'
#!/bin/sh
printf '%s\n' "${DB_URL:-}" > "$CAPTURE_DIR/url"
printf '%s\n' "${DB_USERNAME:-}" > "$CAPTURE_DIR/user"
printf '%s\n' "$@" > "$CAPTURE_DIR/args"
MOCK
chmod +x "$TEST_DIR/java"

CAPTURE_DIR="$TEST_DIR" PATH="$TEST_DIR:$PATH" \
  RENDER_DATABASE_URL='postgresql://growlab:p%3Ass%40word@dpg-private.internal:5432/growlab_payment_staging' \
  DB_USERNAME='growlab' DB_PASSWORD='test-only' PORT='10000' sh "$SCRIPT"
[ "$(cat "$TEST_DIR/url")" = 'jdbc:postgresql://dpg-private.internal:5432/growlab_payment_staging' ]
grep -q -- '-Dserver.port=10000' "$TEST_DIR/args"

CAPTURE_DIR="$TEST_DIR" PATH="$TEST_DIR:$PATH" \
  DB_URL='jdbc:postgresql://db:5432/growlab' \
  DB_USERNAME='growlab' DB_PASSWORD='test-only' sh "$SCRIPT"
[ "$(cat "$TEST_DIR/url")" = 'jdbc:postgresql://db:5432/growlab' ]
grep -q -- '-Dserver.port=8080' "$TEST_DIR/args"

if CAPTURE_DIR="$TEST_DIR" PATH="$TEST_DIR:$PATH" \
   RENDER_DATABASE_URL='postgres://wrong-scheme' DB_USERNAME='growlab' \
   DB_PASSWORD='test-only' sh "$SCRIPT" >/dev/null 2>&1; then
  echo 'Malformed Render URL unexpectedly accepted' >&2
  exit 1
fi
printf 'deployment-entrypoint checks OK\n'
