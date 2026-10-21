#!/bin/sh
# Compatible with local compose (DB_URL supplied) and Render's split Postgres env vars.
# Never print connection credentials into deployment logs.
set -eu

if [ -n "${RENDER_DB_HOST:-}" ]; then
  : "${RENDER_DB_PORT:?Missing Render database port}"
  : "${RENDER_DB_NAME:?Missing Render database name}"
  : "${DB_USERNAME:?Missing database username}"
  : "${DB_PASSWORD:?Missing database password}"
  export DB_URL="jdbc:postgresql://${RENDER_DB_HOST}:${RENDER_DB_PORT}/${RENDER_DB_NAME}"
fi

: "${DB_URL:?DB_URL or Render database fields are required}"
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

# Render exposes a dynamic PORT (10000 by default); Compose uses 8080.
exec java -XX:MaxRAMPercentage=70 -Dserver.address=0.0.0.0 -Dserver.port="${PORT:-8080}" -jar /app/app.jar
