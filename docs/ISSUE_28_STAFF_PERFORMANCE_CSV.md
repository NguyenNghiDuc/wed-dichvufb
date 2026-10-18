# Issue #28 — bounded STAFF activity export

Admin-only `GET /admin/staff/performance.csv` exports the same top-20 STAFF progress event counters as the Admin staff performance page. UTF-8 BOM, `Cache-Control: no-store`, fixed filename, one row per event actor, no individual customers or salaries.

Historical event actors are escaped for CSV quoting and spreadsheet formula injection, including initial spaces. Unlike the time series of orders, these are **audited progress events**, not real paid revenue, compensation, quality assessments or active employee attendance.

MockMvc checks malicious actor escaping, non-STAFF-event exclusion and ADMIN-only authorization versus USER/STAFF/anonymous.
