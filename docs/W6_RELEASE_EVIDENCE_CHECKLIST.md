# W6 sandbox release evidence checklist

This is a **review worksheet**, not an approval or proof of production readiness. Issue #30 / Draft PR #37.

## Automated evidence (record exact HEAD, never rely on an older green run)
- [ ] Record branch HEAD SHA and compare it to the PR head before review.
- [ ] Java 17 Maven verification: record run URL, test count and failures.
- [ ] PostgreSQL 16/Flyway: record applied migration versions, checksums and validation result.
- [ ] CI-only pg_dump/pg_restore: compare restored row counts, receipt totals and required indexes. **Not** a real-backup recovery drill.
- [ ] Production-profile JAR health smoke, Docker image/container health, Compose configuration: record run URL.
- [ ] CodeQL: inspect diagnostics and SARIF upload, not merely workflow conclusion. Track CSRF findings separately.

## Manual sandbox acceptance (requires independent reviewer)
- [ ] Missing, malformed, stale and future timestamp signatures reject without mutations.
- [ ] Exact raw-body HMAC and strict JSON parsing reject duplicate keys, unknown fields and oversized requests.
- [ ] Replayed event and concurrent callbacks cannot create two receipts or double-credit any balance.
- [ ] Wrong owner, amount, state or duplicate event reference is rejected and audited without leaking customer details.
- [ ] Verify admin reconciliation endpoint is read-only, private, no-store; non-admin and anonymous access denied.
- [ ] Confirm rollback and restore on **authorized staging data**, including recovery time and data integrity.
- [ ] Run realistic load and query-plan review on staging with synthetic accounts; log throughput, p95 latency and errors.
- [ ] Complete independent CSRF/security review and mobile UAT; record reviewer, date, evidence and unresolved findings.

## Release stop conditions
Do not merge, deploy, connect real VNPay/bank adapters, or process real funds while any required review is missing. Never put credentials, customer data, full webhook payloads or signature material in comments or artifacts. Keep PR #37 Draft until human sign-off.

## Evidence record template
| Gate | HEAD SHA | Run / artifact URL | Result | Reviewer / date | Blocker |
| --- | --- | --- | --- | --- | --- |
| Java CI | | | Pending review | | |
| CodeQL diagnostics | | | Pending review | | |
| PostgreSQL restore rehearsal | | | Pending review | | |
| Security review | | | Pending | | |
| Staging UAT / load | | | Pending | | |
