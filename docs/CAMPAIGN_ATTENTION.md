# Admin campaign review indicators — issue #3 / roadmap #039

**Status:** Implemented in draft only, not production approved.

## Scope
- Dashboard counts orders still `PENDING_REVIEW` created **more than 48 hours ago** and `IN_PROGRESS` not updated for **more than 168 hours**.
- These provisional thresholds indicate items to *review*, not service level agreement breaches or guaranteed deadline violations.
- Metrics are read-only database counts; no cron, automated status change, payment handling, customer messaging or actual revenue changes.
- Admin dashboard links to the full list for the matching status; the linked page is not a pre-filtered "stale only" list.
- PostgreSQL Flyway V8 adds btree indexes on `(status,created_at)` and `(status,updated_at)`.

## Verification before completion
- H2 MockMvc integration test checks counts, fresh vs aging orders, Admin-only access.
- PostgreSQL CI test checks query against real schema and confirms no state change.
- Java CI including Flyway V8 migration, production JAR smoke, Docker build and CodeQL review required.
- Security issue: the existing CodeQL rule `java/csrf-unprotected-request-type` has previously logged SARIF exporter graph errors, even with workflow success. Remains under manual triage.

## Owner sign-off needed
- Confirm aging thresholds and whether paused or assigned orders need special treatment.
- Staging UAT and time-zone/clock consistency review; monitor query performance with real data.
- Review migration backups, recovery/rollback, and JaCoCo/security alerts before merge.
- No roadmap completion checkmark without evidence and approval.
