# Issue #26 — advanced legacy customer order filters

The older internal-only customer order history now supports exact service name, optional numeric order ID, and inclusive from/to creation dates, all scoped by authenticated owner in the SQL query.

- `GET /order/list?service=<exact-name>&from=YYYY-MM-DD&to=YYYY-MM-DD` with 20 orders/page.
- `GET /order/export.csv` with the same query parameters and a 5,000 row hard limit.
- Service name matching is exact (no wildcard query). Invalid reversed/out-of-range dates, overlong names and nonpositive IDs are rejected with HTTP 400.
- Inclusive dates use the server-local timezone and an exclusive next-day bound. ID and owner predicates are combined with date/service filters.
- The web keeps filters in previous/next URLs; CSV export carries them forward. No schema change or Admin/chat/payment modifications.
- MockMvc integration regressions check cross-account isolation, service/date/ID accuracy, malformed filters, CSV consistency and login requirement.
