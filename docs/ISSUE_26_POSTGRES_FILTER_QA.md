# PostgreSQL tests for Issue #26

The `PostgresCustomerIndexesTest` regression runs on the disposable PostgreSQL 16 GitHub Actions service with the production Flyway profile. It checks the owner-date paging index (V10), the one-review constraint (V11), the customer contact table (V12), and actual owner/service/date filtering.

The test does not modify Admin, chat or payment code, and it must never be run against a live database with production data. Index existence does not by itself prove query performance; production-scale EXPLAIN ANALYZE and browser UAT remain required.
