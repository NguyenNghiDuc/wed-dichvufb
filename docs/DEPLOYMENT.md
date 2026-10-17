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
