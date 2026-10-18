# Issue #28 — bounded priority queue CSV

`GET /admin/attention/export.csv?queue=OVERDUE_PENDING&page=0` downloads the same **single 20-row page** visible on the Admin priority queue. The existing query applies the queue/status/staleness criteria inside SQL; no full backlog export or customer data retrieval is added.

CSV includes campaign ID, status, service type, created/updated times and assignee username only. No customer ownership, target URL, brief, email, phone, password, payment details or actual revenue. Text fields use formula-safe CSV escaping, with UTF-8 BOM, fixed filename, `Cache-Control: no-store` and `nosniff`.

MockMvc tests cover valid overdue/fresh filtering, row count, malformed enum/page, role ADMIN/STAFF/USER/anonymous, and UI export link. No queue mutation or automatic SLA enforcement. Browser/Excel/manual staging acceptance remains pending.
