#!/bin/sh
# Render Postgres has one supported "connectionString" property, not host/port.
# Extract the hostname/path into the JDBC URL; credentials are provided separately.
# Local Docker Compose continues to use DB_URL directly.
set -eu

if [ -n "${RENDER_DATABASE_URL:-}" ]; then
  case "$RENDER_DATABASE_URL" in
    postgresql://*@*/*)
      connection="${RENDER_DATABASE_URL#postgresql://}"
      host_and_database="${connection#*@}"
      case "$host_and_database" in
        *'@'*|*' '*|'')
          echo 'Invalid Render PostgreSQL URL' >&2
          exit 1
          ;;
      esac
      export DB_URL="jdbc:postgresql://$host_and_database"
      ;;
    *)
      echo 'Invalid Render PostgreSQL URL scheme or structure' >&2
      exit 1
      ;;
  esac
fi

: "${DB_URL:?DB_URL or RENDER_DATABASE_URL is required}"
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

# Render listens on PORT=10000 (default), Compose on 8080.
exec java -XX:MaxRAMPercentage=70 -Dserver.address=0.0.0.0 -Dserver.port="${PORT:-8080}" -jar /app/app.jar
