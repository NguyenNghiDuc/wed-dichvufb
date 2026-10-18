# GrowLab W3 — Issue #27 implementation status

All changes are in draft PR #35, not production or merged main.
Existing chat pagination PR #23 was merged already and reused.

Implemented candidates:
- owner-scoped 2-way thread pages (20 default, cap 50), conversation search/index and REST contacts
- grouped unread count query, atomic read receipt and generic recipient notification
- white/orange responsive chat UI linked from customer/staff support screens
- support-ticket paged history, search, status/priority, staff claim and Admin reassignment
- append-only assignment audit with 50-row bounded display
- customer-to-staff reply notifications without untrusted message bodies
- bounded rate controls using the current in-process TrafficLimiter
- Flyway V20–V25 support/chat indexes and migrations; PostgreSQL concurrency tests

Validate the latest HEAD Java/PostgreSQL CI and CodeQL; review the prior CodeQL
SARIF issue #13. Run mobile/browser UAT, restore tests, real load and retry UX.
The in-process rate limiter is not a distributed protection when scaling replicas.
Never label this as production-ready solely because CI is green.
