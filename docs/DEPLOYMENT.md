# GrowLab — Deployment and release checklist

## Phase 2: Database
- Dev runs file-based H2 with `spring.flyway.enabled=false`.
- Production uses PostgreSQL 16 + Flyway, Hibernate `ddl-auto=validate`.
- Baseline clean Postgres DB migrations V1/V2/V3. **Existing H2 data is not copied**. Export and reconcile any real users, orders, and deposits manually before cutover. Never rename legacy resource folders and assume old migrations were run.
- Backup PostgreSQL volume before production upgrades, test recovery on staging.

## Phase 3: Official APIs
- YouTube Data API public read-only channel summary is implemented at `GET /api/v1/integrations/youtube/channels/UC... (24-char ID)`; requires server-side `YOUTUBE_API_KEY`. Calls are disabled without key.
- Meta and TikTok connectors are NOT live: OAuth app approval, permission review, provider-specific terms and test accounts are prerequisites.
- Never automate artificial engagement or use session cookies/passwords of social network accounts.

## Phase 4: AI & calendar
- The content calendar only stores DRAFT/PLANNED; it **does not publish posts automatically**.
- Caption endpoint `POST /api/v1/ai/caption` uses official OpenAI endpoint only when `GROWTH_AI_API_KEY` is set. No key -> 503. Add per-user quota before broad public launch.
- Do not submit secrets or sensitive customer data to prompt text. Review output before posting.

## Phase 5: Payments
- `POST /api/v1/payments/webhooks/internal` is a **generic internal signed event interface**; it is NOT a bank or VNPay integration. Without `GROWTH_WEBHOOK_SECRET` of 32+ bytes the endpoint returns 503.
- Header `X-GrowLab-Signature` = lowercase HMAC-SHA256 hex digest of raw UTF-8 JSON body. Payload: `{"eventId":"unique-event-123","depositRequestId":123,"amountVnd":50000,"status":"SETTLED"}`.
- A signed event checks deposit amount, row lock, idempotency, and updates status PENDING→APPROVED. It does not credit wallet balance. Real payment provider authentication, transaction reconciliation and refunds require a separate approved adapter.

## Phase 6: Run/test/deploy
1. Clone, run `mvn -B clean verify` with PostgreSQL running on localhost:5432 for production schema integration tests; CI provisions DB automatically.
2. Generate local secrets, set `POSTGRES_PASSWORD` (do not commit). `docker compose up --build -d`.
3. Terminate TLS at trusted reverse proxy and configure secure cookies. App binds port 8080 to loopback only.
4. Validate `/actuator/health`, registration/login/logout, CSRF, ownership, admin roles, order state transitions, scheduling, notifications, read-only YouTube test, disabled AI behavior, webhook bad signature, duplicate event, database backup.
5. Apply rate limits, transactional outbox for third-party calls, environment-specific logging/monitoring, OAuth/API review, production security assessment before opening public access.
6. Review CI and PR, and merge only once production review passes. Main branch is not automatically changed.

## Minimal smoke sequence
- Open `/`, `/register`, `/login`
- Register A & B; create order A; verify B cannot access A by ID
- Confirm normal USER gets HTTP 403 from `/admin/campaign-orders`
- Grant admin through controlled DB ops; check state changes and audit/notifications
- Verify all POSTs require CSRF other than signed internal webhook
- Verify production profile fails startup with missing DB credentials
- Verify no third-party call happens when provider secrets are absent

## W10 staging/operational rehearsal — draft PR #49 (no production deployment)

- Read [Staging HTTPS, preflight, health and rollback](OPERATIONS_STAGING.md) and [isolated PostgreSQL backup/restore runbook](OPERATIONS_BACKUP_RESTORE.md) before any release. These are test procedures, **not proof staging has been deployed**.
- The default `compose.yaml` remains local-only and binds app HTTP to `127.0.0.1:8080`. Opt in to `compose.staging.yaml` **only** with approved staging DNS, environment secrets and an operator-controlled HTTPS reverse proxy.
- Run offline syntax/fail-closed verification: `bash scripts/ops/test_guardrails.sh`. If an isolated Docker Engine is available and authorized, run `bash scripts/ops/test_backup_restore.sh` using synthetic data only. Never replace real database contents to test scripts.
- `bash scripts/ops/preflight_staging.sh` validates staging config without starting containers; `bash scripts/ops/smoke.sh --local` and `--tls` perform GET-only health/login checks; `bash scripts/ops/rollback_plan.sh --plan CURRENT_SHA PREVIOUS_SHA` prints instructions without executing deployment or restore.
- **Not accepted for production:** W9 CI/CodeQL must be reviewed for latest HEAD; W7 migrations and W6 payments need their own approvals; no actual RPO/RTO, offsite encryption, alerts, secrets manager or disaster-recovery rehearsal has been verified.
