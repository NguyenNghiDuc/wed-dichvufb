# Internal sandbox payment security, reconciliation and recovery (Issue #30)

## Scope and protocol
This is **not** a VNPay IPN/return callback, bank transfer confirmation, customer wallet or cash ledger. No real settlement or wallet credit happens. Keep `GROWTH_WEBHOOK_SECRET` unset outside authorized sandbox/testing; without a secret (32+ UTF-8 bytes) the webhook returns HTTP 503. A secret must be random, securely stored and rotated using an agreed maintenance window; never include it in URL, logs, PR or test fixtures outside local mock keys.

`POST /api/v1/payments/webhooks/internal` accepts only `Content-Type: application/json` and payloads up to 4096 raw bytes. Required headers:
- `X-GrowLab-Timestamp`: ASCII Unix time **seconds**, decimal (10–12 digits), within 300 seconds before or after server time.
- `X-GrowLab-Signature`: exactly 64 hex characters; `hex(HMAC-SHA256(secret, ASCII(timestamp) || "." || exact raw HTTP body bytes))`.

Example body **shape only** (no valid signature): `{"eventId":"sandbox-settlement-0001","depositRequestId":1,"amountVnd":50000,"status":"SETTLED"}`. All fields are mandatory; eventId is 8–100 ASCII letters/digits/underscore/hyphen. Generate a new signature and fresh timestamp for each retry after five minutes; **reuse the original eventId** so a retry cannot duplicate the effect. Keep producer and receiver clocks synchronized using NTP. Do not reserialize JSON between signing and sending.

The service checks the signature before parsing, locks the deposit with `PESSIMISTIC_WRITE`, compares the exact VND amount, enforces the allowed status transition PENDING → APPROVED and commits a single receipt row. The database additionally enforces UNIQUE(event_id) and UNIQUE(deposit_request_id). One accepted event generates one account notification. Duplicate retry with same eventId, amount and deposit returns `{"accepted":true,"duplicate":true}` without another receipt/notification. A different event claiming a processed deposit returns HTTP 409. Invalid/missing signature, stale timestamp: 401; malformed event: 400; mismatched reference/amount: 409; no deposit: 404; no secret: 503. **APPROVED means only an authorized internal test event was recorded, not that money arrived.** It must not be treated as payment confirmation by any future wallet logic.

## Read-only daily reconciliation (PostgreSQL, authorized staff only)
Run in a read-only connection on staging/approved replica, review anomalies without automatically crediting or adjusting balances:

```sql
-- Approved deposit with no matching immutable internal receipt.
SELECT d.id, d.owner_username, d.amount, d.status
FROM deposit_request d LEFT JOIN payment_receipts r ON r.deposit_request_id = d.id
WHERE d.status = 'APPROVED' AND r.id IS NULL;

-- Receipt linked to wrong amount or non-approved state. Investigate manual edits / migration.
SELECT r.id, r.event_id, r.deposit_request_id, r.amount_vnd,
       d.amount AS requested_vnd, d.status
FROM payment_receipts r LEFT JOIN deposit_request d ON d.id = r.deposit_request_id
WHERE d.id IS NULL OR d.amount <> r.amount_vnd OR d.status <> 'APPROVED';

-- The last day's receipt count and total are internal event totals, NOT bank revenue.
SELECT COUNT(*) AS internal_settlements, COALESCE(SUM(amount_vnd),0) AS internal_vnd
FROM payment_receipts WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '1 day';

-- Should return 0 rows (also constrained by the database).
SELECT event_id, COUNT(*) FROM payment_receipts GROUP BY event_id HAVING COUNT(*) > 1;
SELECT deposit_request_id, COUNT(*) FROM payment_receipts
GROUP BY deposit_request_id HAVING COUNT(*) > 1;
```

Do not claim financial revenue from internal settlement events. Real VNPay requires its own sandbox credential, provider-specific checksum/response semantics, merchant-order mapping, webhook acknowledgement/retries and reconciliation against signed provider records, plus owner approval.

## Flyway / indexes / deployment
Clean PostgreSQL schema is managed with versioned `src/main/resources/db/migration/V*.sql` (production `validate`; no production Hibernate auto-DDL). V3 has receipt uniqueness; V10 adds indexes `idx_deposit_owner_created` and `idx_payment_receipts_created_at`. NEVER edit checksums of applied Flyway scripts or run Flyway `repair` automatically. Inspect `flyway_schema_history` and review query plans on realistic staging data. For owner listing inspect `EXPLAIN (ANALYZE, BUFFERS)` with sampled but redacted data, not live personally identifying data in tickets.

## Backup and restore rehearsal — operator-controlled, not automatic
Before any migration take a consistent PostgreSQL custom-format backup **outside the repo**. Example, with PGHOST, PGPORT, PGUSER, PGDATABASE and PGPASSWORD securely supplied by the operator:

```sh
umask 077
pg_dump --format=custom --no-owner --file=/secure/backups/growlab-before-upgrade.dump
pg_restore --list /secure/backups/growlab-before-upgrade.dump > /secure/backups/manifest.txt
```

Copy encrypted backup to controlled storage, verify retention and checksum, and rehearse restore to an **empty isolated staging database only**. Example after configuring `PGDATABASE=growlab_restore` on a nonproduction cluster:

```sh
createdb growlab_restore
pg_restore --no-owner --no-privileges --dbname=growlab_restore /secure/backups/growlab-before-upgrade.dump
psql --dbname=growlab_restore -c 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank'
psql --dbname=growlab_restore -c 'SELECT COUNT(*) FROM payment_receipts'
```

`pg_restore` against an existing production database is not an approved rollback. Document the actual tested recovery point objective, recovery time, roles, restore checksums and encryption key availability after a staging exercise. On failed deployment, stop the new app and route traffic back to previous compatible JAR/container **only if schema remains backward-compatible**; for destructive migrations, restore the isolated validated backup via a separate human-approved incident procedure. Never use `flyway clean` in production.

## Acceptance evidence remaining
- [ ] Latest Java 17 Maven test run with PostgreSQL 16 + Flyway V1–V10.
- [ ] Parallel payment callbacks: same deposit/different eventId and same eventId/different deposit.
- [ ] Production JAR startup + Docker build smoke tests at same commit.
- [ ] CodeQL findings reviewed, including unresolved CSRF analyzer diagnostic (green workflow alone not clearance).
- [ ] Restore rehearsal using an actual staging snapshot and measured recovery time.
- [ ] Manual security review, multi-user load test and HTTPS/staging UAT.
- [ ] VNPay sandbox configuration and independent provider-contract verification (NOT enabled).
