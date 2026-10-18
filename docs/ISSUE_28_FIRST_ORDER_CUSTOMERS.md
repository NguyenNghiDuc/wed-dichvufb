# Issue #28 — first-time campaign customers (not new registrations)

Because the existing `users` schema has **no account registration timestamp**, W4 cannot truthfully report new registered accounts without a separately approved migration owned by W7.

The dashboard therefore displays a precisely labeled proxy: distinct customer usernames whose **first-ever campaign order** falls in the past 30 days, based on historical `campaign_orders.created_at`. The anti-prior-order predicate is applied in the database; multiple new orders do not double-count, older owners returning in this month do not count.

`GET /api/v1/admin/analytics/first-order-customers` is ADMIN-only, no-store, and returns an explicit metric definition. No usernames, user PII, payments, authentication changes or migrations.

MockMvc/H2 tests cover multiple new orders, previously existing owners, another first-time owner and ADMIN-only access. For authoritative signup stats, coordinate a future users.created_at migration with W7 and preserve historical data.
