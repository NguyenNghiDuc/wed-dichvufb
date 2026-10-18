# Issue #28 — STAFF-only self dashboard

STAFF/ADMIN members use `GET /staff/overview` to see current assigned orders by status (PENDING_REVIEW, APPROVED, IN_PROGRESS, COMPLETED) and their own recorded start/completion actions in the last 30 days. A single owner-scoped grouped SQL query returns status counts; 30-day actions derive from the already-audited CampaignEvent records.

`GET /staff/overview.json` returns the exact same metrics with no-store. Both endpoints deliberately live under `/staff/**` covered by existing STAFF/ADMIN Spring Security rules; there is **no new /api/v1/staff/** route** that could be accessible to arbitrary authenticated users. The service independently checks the current account is enabled STAFF/ADMIN. Request-supplied username is never accepted.

MockMvc tests cover assigned versus another employee's campaigns, forged username, actual STAFF event actor counters, disabled staff, USER and anonymous. No write, no accounting ledger, no edits to auth or payments. Browser/tablet/mobile UAT pending.
