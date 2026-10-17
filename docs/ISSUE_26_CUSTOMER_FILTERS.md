# Issue #26 — customer advanced filters (18 October 2026)

- Web: `/campaigns?orderId=&status=&serviceType=CONTENT_PLAN&from=2026-10-01&to=2026-10-18`.
- API: `GET /api/v1/campaign-orders/page?serviceType=...&from=YYYY-MM-DD&to=YYYY-MM-DD`.
- CSV: `GET /campaigns/export.csv` uses the **same** service/status/ID/date filters; remains bounded to 5,000 records and sanitized.
- Date range is inclusive, based on the JVM application's local timezone, with a next-day exclusive boundary; dates restricted to 2000–2100 to avoid overflow.
- Query always constrains `ownerUsername` from authenticated `Principal`; never allows a supplied owner. All sort columns are server-defined.
- Exact enums reject unrecognized values with 400; reversed dates reject with 400.
- PostgreSQL V10 adds an additive owner/date/ID paging index. Review EXPLAIN ANALYZE on staging data before acceptance.
- Automated H2/CI tests validate date + service filtering, CSV consistency, ownership isolation, invalid inputs, anonymous access.
- Production signoff still requires browser testing, staging DB migration, and human review.
