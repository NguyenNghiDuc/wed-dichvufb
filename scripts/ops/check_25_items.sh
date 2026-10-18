#!/usr/bin/env bash
# Verifies exactly 25 W10 implementation contracts; no deployment or Docker commands.
# IMPORTANT: 25/25 CODE checks is NOT production/staging acceptance.
set -Eeuo pipefail
umask 077
[[ $# == 0 && -f compose.yaml && -f docs/OPERATIONS_W10_25_CHECKLIST.md ]] || {
  echo 'Run this without arguments from the W10 repository root' >&2
  exit 2
}
count=0
contains_all() {
  local path="$1" needle
  shift
  [[ -f "$path" ]] || return 1
  for needle in "$@"; do grep -Fq -- "$needle" "$path" || return 1; done
}
check() {
  local num="$1" title="$2"
  shift 2
  if "$@"; then
    count="$((count+1))"
    printf '[%02d/25] CODE OK: %s\n' "$num" "$title"
  else
    printf '[%02d/25] INCOMPLETE: %s\n' "$num" "$title" >&2
    exit 1
  fi
}
control08() {
  contains_all compose.staging.yaml 'db_private:' 'internal: true' 'networks: [db_private]' 'web_private:'
}
control10() {
  contains_all compose.staging.yaml 'max-size: "10m"' 'max-file: "3"' &&
    ! grep -Fq '  log {' ops/Caddyfile.staging
}
control19() {
  contains_all compose.recovery.yaml 'fixture-db:' 'recovery-db:' 'tmpfs:' 'internal: true' &&
    ! grep -Fq '    ports:' compose.recovery.yaml &&
    contains_all scripts/ops/restore_sandbox.sh 'exec -T recovery-db' 'growlab_recovery'
}
control22() {
  [[ -f scripts/ops/test_guardrails.sh && -f scripts/ops/test_preflight.sh &&
     -f scripts/ops/test_smoke.sh && -f scripts/ops/test_monitor.sh ]]
}
control24() {
  contains_all scripts/ops/monitor.sh '/actuator/health/readiness' '/actuator/health/liveness' 'backup_inventory.sh' &&
  contains_all scripts/ops/smoke.sh '--tls' '/login' '/actuator/health' &&
  contains_all scripts/ops/rollback_plan.sh 'NO COMMANDS EXECUTED' 'PREVIOUS_KNOWN_GOOD_SHA'
}
control25() {
  [[ -s docs/DEPLOYMENT.md && -s docs/OPERATIONS_STAGING.md &&
     -s docs/OPERATIONS_BACKUP_RESTORE.md && -s docs/OPERATIONS_W10_25_CHECKLIST.md ]] &&
    grep -Fq 'RPO' docs/OPERATIONS_BACKUP_RESTORE.md &&
    grep -Fq 'approval' docs/OPERATIONS_STAGING.md
}

check 01 'Non-root container and ownership' contains_all Dockerfile 'USER 10001:10001' '--chown=10001:10001'
check 02 'Java17 and JVM memory/OOM budget' contains_all Dockerfile '17-jre-alpine' 'MaxRAMPercentage=65' 'ExitOnOutOfMemoryError'
check 03 'Docker health gate' contains_all Dockerfile 'HEALTHCHECK' '/actuator/health' '--start-period=65s'
check 04 'PostgreSQL 16 runtime and fixtures' contains_all compose.yaml 'postgres:16-alpine' 'pgdata:'
check 05 'DB healthy before app' contains_all compose.yaml 'pg_isready' 'service_healthy'
check 06 'Manual staging HTTPS reverse proxy' contains_all compose.staging.yaml 'caddy:2.10.2-alpine' '443:443' 'STAGING_HOST:'
check 07 'Quiet staging DNS/secret preflight' contains_all scripts/ops/preflight_staging.sh 'STAGING_HOST' 'POSTGRES_PASSWORD' 'config --quiet'
check 08 'Private staging database network' control08
check 09 'Container memory/CPU/PID controls' contains_all compose.staging.yaml 'mem_limit:' 'cpus:' 'pids_limit:' 'read_only: true'
check 10 'Log rotation and HTTP access privacy' control10
check 11 'Production JDBC/Flyway validation' contains_all src/main/resources/application-prod.properties 'spring.datasource.url=' 'spring.flyway.enabled=true' 'spring.jpa.hibernate.ddl-auto=validate'
check 12 'HTTPS secure cookies and proxy headers' contains_all src/main/resources/application-prod.properties 'server.servlet.session.cookie.secure=true' 'server.forward-headers-strategy=framework'
check 13 'Masked health/readiness/liveness configuration' contains_all src/main/resources/application-prod.properties 'management.endpoint.health.probes.enabled=true' 'management.endpoint.health.show-details=never' 'management.endpoints.web.exposure.include=health,info'
check 14 'Graceful application shutdown' contains_all src/main/resources/application-prod.properties 'server.shutdown=graceful' 'spring.lifecycle.timeout-per-shutdown-phase=25s'
check 15 'Explicit app-database backup consent' contains_all scripts/ops/backup.sh 'GROWLAB_BACKUP_CONFIRM' 'Refusing app database backup'
check 16 'Private external owned backup location' contains_all scripts/ops/backup.sh 'BACKUP_DIR' 'repo_root=' 'stat -c' 'operator-owned private backup directory'
check 17 'Portable archive and checksums' contains_all scripts/ops/backup.sh 'pg_dump --format=custom' 'pg_restore --list' 'sha256sum'
check 18 'Failure cleanup of incomplete backup pair' contains_all scripts/ops/backup.sh 'complete=false' 'cleanup()' 'rm -f -- "$published" "$published.sha256"'
check 19 'Ephemeral, sandbox-only restore destination' control19
check 20 'Sandbox checksum, empty-target and transactional restore' contains_all scripts/ops/restore_sandbox.sh 'Checksum mismatch' 'Recovery sandbox is not empty' '--single-transaction' '--exit-on-error'
check 21 'Synthetic Docker round trip with unique project' contains_all scripts/ops/test_backup_restore.sh 'COMPOSE_PROJECT_NAME=' 'w10_fixture_test' 'count' 'sum'
check 22 'Independent offline negative-test suites' control22
check 23 'Backup inventory using UTC capture time, private file modes' contains_all scripts/ops/backup_inventory.sh 'BASH_REMATCH[1]' 'directory_mode=' 'checksum mismatch'
check 24 'Health, smoke and safe rollback tools' control24
check 25 'Operator deployment/recovery/approval docs' control25

# Every file, not just 25 source excerpts, is parsed by Bash without execution.
for script in scripts/ops/*.sh; do
  bash -n "$script" || { echo "FAIL: Bash syntax in $script" >&2; exit 1; }
done
bash scripts/ops/test_guardrails.sh
bash scripts/ops/test_secret_guard.sh
bash scripts/ops/secret_guard.sh
[[ "$count" -eq 25 ]] || { echo "FAIL: counted $count/25" >&2; exit 1; }
echo '25/25 W10 CODE CONTRACTS AND OFFLINE NEGATIVE TESTS PASS.'
echo 'NOT ACCEPTED: real Docker fixture restore, live TLS, staging UAT, CI/CodeQL review, production approval.'
