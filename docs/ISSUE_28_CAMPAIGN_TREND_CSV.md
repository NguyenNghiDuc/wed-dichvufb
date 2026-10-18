# Issue #28 — trend chart CSV export

Admin-only `GET /admin/reports/campaign-trend.csv?period=DAY|WEEK|MONTH` exports exactly the same bounded aggregation shown on the dashboard and read-only trend JSON API. The CSV rows contain bucket start dates, order creation counts and current-completed-order counts, with a UTF-8 BOM and `Cache-Control: no-store`.

All cells are generated numeric/date fields, never customer-supplied text, usernames, email or quotes; filenames are derived from a closed enum. Unauthorized requests are blocked via existing `/admin/**` rule; invalid period yields 400.

No actual cash/payment revenue is inferred, no migrations or core auth/payment changes. MockMvc tests cover permission, limited row counts, dashboard link, CSV/JSON consistency and PII exclusion.
