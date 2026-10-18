# Issue #28 — Admin order trend chart

Admin dashboard now renders a responsive, accessible native HTML progress-bar chart. Periods are **14 days**, **12 ISO weeks** (Monday start), and **12 calendar months**. A single bounded SQL daily aggregation over campaign_orders handles the chosen window; Java rolls up at most 366 daily aggregates to 12–14 display buckets and fills zero-volume periods.

Only actual created-order counts and number of those orders **currently COMPLETED** appear. This is **not revenue**, payments, or a daily completion-event tally. No user PII, browser scripts, payment records or migrations are accessed.

Authenticated read-only `GET /api/v1/admin/analytics/campaign-trend?period=DAY|WEEK|MONTH` uses ADMIN security rules, `Cache-Control: no-store`, rejects unknown periods with HTTP 400. Tests cover daily buckets, nonempty and zero buckets, grouping for weeks/months, HTML, JSON and ADMIN-only boundaries.

Dates use application server local timezone; align timezone with operations reporting and benchmark the SQL grouping on staging data.
