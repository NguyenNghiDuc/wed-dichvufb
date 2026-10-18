# Issue #26 — PostgreSQL legacy order query indexes

Flyway V13 adds two B-tree indexes to improve owner-scoped stable pagination, date filters and exact service-name lookup:

- `idx_orders_customer_owner_created`: `(owner_username, created_at DESC, id DESC)`.
- `idx_orders_customer_owner_service_created`: `(owner_username, server_name, created_at DESC, id DESC)`.

Both are additive; the original owner index remains. `PostgresCustomerIndexesTest` verifies them against the PostgreSQL 16 service in GitHub CI. These indexes do not prove production-scale latency: benchmark with `EXPLAIN (ANALYZE, BUFFERS)` and inspect write overhead on staging. Plan backup/maintenance before applying non-concurrent index migration to any live instance.
