# W4 · Admin & Staff operations acceptance (#28)

**Status:** IMPLEMENTED CANDIDATE / NOT PRODUCTION ACCEPTED. Every referenced roadmap item remains [ ] until review and real-world UAT.

## What existed before W4
- Admin accounts USER/STAFF/ADMIN, enable/disable, account audit and anti-self-edit safeguards.
- Catalog prices with bounds, optimistic @Version, change history, quoted amounts fixed at order creation.
- Order status transitions, pessimistic locks, assignment events, owner notifications and staff progress constraints.
- Admin support inbox and ticket responses; dashboard 48-hour and 7-day provisional review indicators.
- Campaign CSV export with 5,000-row cap. These are prior prototypes, not newly implemented by W4.

## New, implemented under Draft PR #36
- **Admin accounts:** exact username / allowlisted role / enabled filter. Query-side predicates, 20-row paging, invalid input 400. Do not treat wildcard, substring or email input as authorized query.
- **Account audit:** immutable read-only page, 20 newest events per page with bounded page index. Audit writes remain in the existing transactional AdminUserService.
- **Admin dashboard:** database aggregate per status, 8 highest *active* staff workloads; a total of *quoted value on open campaigns*, excluding rejected/cancelled/completed. This is never recognized revenue and is not proof of payment.
- **Admin CSV:** counts + status aggregates + open pipeline quotes only; no customer email/username or personal data. Browser download no-store.
- **Staff:** status filter on assigned orders with DB-side assignee scope, 20-row paging, no arbitrary staff selector. Existing CSRF and row-locked transitions preserved.
- **UI:** /admin.css scoped to admin/staff templates, white–orange responsive cards, table scroll, keyboard focus styles, no external fonts/scripts. No customer CSS changes.

## Required evidence before approval
- Run Java 17/Maven tests including H2 role tests and PostgreSQL/Flyway/JAR/Docker, and inspect actual log results for PR HEAD.
- Review CodeQL uploaded alerts and the persistent `java/csrf-unprotected-request-type` SARIF graph errors (issue #13). A green analysis job is **not security clearance**.
- Test interactive behavior in mobile and desktop browsers; accessibility, text overflow, touch targets and admin task confirmation.
- Independent security review of all role/CSRF/change-history paths, account-lock race conditions and invoice/quote boundaries.
- Owner to approve thresholds for late/stale orders, payment ledger reconciliation, real revenue reporting and rollback/backup plan.
- Review query plans with realistic dataset, including indexes on user role/enabled and assignment status; do not claim production performance measurements from CI alone.

## Not implemented / owner dependent
- Actual collected revenue/settled-payment reporting. Current CSV has **no revenue figure**.
- XLSX workbook output, bulk system notifications, multi-server rate limiting, device/IP history and real refund workflows.
- Email, payment gateway, real API credentials and production hosting. These require owner configuration and acceptance.

**No production deployment, no actual financial operations, no fake follower/engagement actions, no secrets.**
