# Issue #28 — Admin activity date window

Admin read-only routes `/admin/activity` and `/admin/activity/export.csv` now accept optional `from=YYYY-MM-DD&to=YYYY-MM-DD` in addition to area and exact actor. Both endpoints use the same service query.

The date range is inclusive in server-local calendar days via `createdAt >= from.atStartOfDay()` and `createdAt < to.plusDays(1).atStartOfDay()`. Accepted years are 2000–2100 and reversed ranges reject with HTTP 400. All predicates run **inside** each audit source query before each 50-row limit; no retrieving broad audit data then filtering in Java.

Fixed entity names/JPQL fragments prevent arbitrary user SQL/HQL injection; caller controls only validated actor, enum and ISO dates. Filter state is retained in the export link. Existing ADMIN-only authorization/no-store/formula escaping remains intact.

MockMvc tests cover exact day boundary conditions, cross-actor exclusion, HTML and CSV consistency, invalid/reversed dates, STAFF/USER/anonymous access. UTC vs server local timezone should be documented in production operations guide.
