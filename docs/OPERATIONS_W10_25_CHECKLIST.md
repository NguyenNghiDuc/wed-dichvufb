# W10 Issue #47 — 25-item delivery and acceptance checklist

**Meaning of "25/25":** Twenty-five W10 implementation controls below must each have source code or a documented operational procedure and a reproducible verification step. **Implementation != acceptance.** Do not claim all 25 production-tested until an operator runs the offline tests, isolated Docker restoration, HTTPS staging smoke, security review and obtains approvals. GitHub CI alone does not authorize deployment.

| # | Required W10 item | Source / verification | Current delivery |
|---|---|---|---|
| 01 | Non-root Java container | `Dockerfile` UID/GID 10001, JAR ownership | Implemented; Docker check pending |
| 02 | Java 17 and bounded JVM/OOM | `Dockerfile` base image/MaxRAM/ExitOnOOM | Implemented; image verification pending |
| 03 | Docker HTTP health check | `Dockerfile` health endpoint/warm-up | Implemented; Docker check pending |
| 04 | PostgreSQL 16 service | `compose.yaml` / `compose.recovery.yaml` | Implemented; engine test pending |
| 05 | PostgreSQL health gating app startup | `compose.yaml` depends_on service_healthy | Implemented; engine test pending |
| 06 | HTTPS reverse proxy for staging | `compose.staging.yaml`, `ops/Caddyfile.staging` | Implemented; real TLS pending |
| 07 | DNS/secret/Compose dry-run validation | `scripts/ops/preflight_staging.sh`, `test_preflight.sh` | Implemented; Bash test pending |
| 08 | Database isolation and no public DB port | staging/recovery Compose networks | Implemented; engine network check pending |
| 09 | RAM/CPU/PID and restart controls | `compose.staging.yaml` | Implemented; load test pending |
| 10 | Log rotation and PII-safe access-log policy | `compose.staging.yaml`, Caddy config | Implemented; staging audit pending |
| 11 | Production PostgreSQL/Flyway schema validation | `application-prod.properties` | Implemented; integration test pending |
| 12 | HTTPS-aware cookies and forwarded headers | `application-prod.properties` | Implemented; proxy UAT pending |
| 13 | Minimal health/live/ready endpoints | `application-prod.properties`, `monitor.sh` | Implemented; probe UAT pending |
| 14 | Graceful shutdown and limited diagnostic output | production config, `Dockerfile` | Implemented; restart test pending |
| 15 | Explicit opt-in before application DB backup | `scripts/ops/backup.sh --db` | Implemented; guardrail test pending |
| 16 | Private external absolute backup directory | `backup.sh`, `test_guardrails.sh` | Implemented; offline test pending |
| 17 | Portable pg_dump archive + SHA-256 | `backup.sh` | Implemented; Docker fixture test pending |
| 18 | Archive/checksum failure cleanup | `backup.sh` traps | Implemented; fault injection pending |
| 19 | Fixed, isolated, synthetic recovery target | `compose.recovery.yaml`, `restore_sandbox.sh` | Implemented; Docker test pending |
| 20 | Restore checksum and no-overwrite checks | `restore_sandbox.sh`, `test_backup_restore.sh` | Implemented; Docker test pending |
| 21 | End-to-end fixture backup→restore drill | `test_backup_restore.sh` in unique Compose project | Implemented; Docker test pending |
| 22 | Offline negative tests (no Docker/network) | `test_guardrails.sh`, `test_preflight.sh`, `test_smoke.sh` | Implemented; local run pending |
| 23 | Read-only backup freshness/integrity monitor | `backup_inventory.sh`, `test_monitor.sh` | Implemented; scheduler not approved |
| 24 | GET-only smoke, rollback plan, health monitor | `smoke.sh`, `monitor.sh`, `rollback_plan.sh` | Implemented; HTTPS/rehearsal pending |
| 25 | Operator runbook, RPO/RTO/approval gates | `docs/DEPLOYMENT.md`, `docs/OPERATIONS_*.md` | Documented; approval pending |

## Reproducible checks

Safe to run from the W10 branch in an ordinary Bash terminal:

```bash
bash scripts/ops/check_25_items.sh
bash scripts/ops/test_guardrails.sh
```

These check **local implementation and mock failure behavior** only. They must not access app DB or real HTTP services.

On a separately approved machine with Docker Compose v2, **synthetic data only**:

```bash
bash scripts/ops/test_backup_restore.sh
```

Then, only with approved staging host, secrets and DNS/TLS:

```bash
bash scripts/ops/preflight_staging.sh
bash scripts/ops/smoke.sh --local
bash scripts/ops/smoke.sh --tls
```

**Must not automatically run:** production deploy, real DB restore, secrets upload, migrations, payment reconciliation, external monitoring installation, DNS switching or live user tests. W9 owns GitHub Actions workflow and W7 owns migrations. Reviewer must check W9 CodeQL SARIF warnings and full PR conflict surface.

### Non-negotiable acceptance evidence

- GitHub Java CI and CodeQL for the exact PR HEAD are complete and reviewed.
- All offline guardrails pass; the Docker-backed fixture drill passes with checksum corruption and second-restore refusal; Docker Compose/Caddy validate.
- Operator runs manual staging HTTPS and restart/rollback rehearsal on disposable data, inspects log privacy, provides incident contact and RPO/RTO evidence.
- Owners/reviewers approve database/migration compatibility, actual secrets system, offsite encryption, retention, monitoring and rollback.
- Only then may statuses be changed from **implemented** to **accepted** and PR #49 be considered for merge. No automatic production action follows a green check.
