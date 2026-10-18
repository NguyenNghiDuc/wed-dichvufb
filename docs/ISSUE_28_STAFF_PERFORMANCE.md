# Issue #28 — 30-day STAFF progress counters

Admin-only `GET /admin/staff/performance` shows the top 20 actors who recorded *staff* start/complete transitions in the previous 30 days. Read-only `GET /api/v1/admin/staff/performance` returns the same summary with `Cache-Control: no-store`.

A bounded SQL aggregate counts `campaign_events` whose description is exactly the established STAFF progress audit event; customer creation and Admin status changes are excluded. An individual actor summary is available internally for a future STAFF-only dashboard. Numbers reflect recorded start/completion transitions, **not timesheets, quality scores, salary/payroll or actual money**.

Tests cover staff-specific audit, excludes unrelated events, zero actor activity, global endpoint ADMIN-only versus STAFF/USER/anonymous. No changes to account roles, auth, assignment mutation, payment or migrations. Browser/staging validation pending.
