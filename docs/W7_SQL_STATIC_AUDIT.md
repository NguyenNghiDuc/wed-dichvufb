# W7 static SQL audit for PR #33 and #37

Scope: review only; no migration file or database modified.

## W2 PR #33
- V10 creates campaign owner/created_at/id index for stable pagination.
- V11 creates campaign_reviews; campaign_id FK references campaign_orders(id); unique campaign_id; rating check 1..5; owner index.
- V12 creates customer_profiles with owner_username as primary key and FK to users(username).
- V13 creates two owner-scoped indexes on orders, one with server_name for service filtering.
- Verify parent table names, column types and users(username) uniqueness against main V1–V9 before migration.

## W6 PR #37
- V10 adds deposit_request owner/time index and payment_receipts created_at index.
- V11 creates stable owner/time/id index and drops the V10 owner/time index. Preserve V10 before V11 when assigning new versions.
- V12 adds NOT VALID amount checks to deposit_request and payment_receipts. Historic rows are not validated; audit before any later VALIDATE CONSTRAINT.
- Confirm that no other branch introduces the same index names.

## W3 PR #35 and #44
See docs/W7_ACCEPTANCE_25.md for V10–V15 support migration inspection. PR #44 proposes V20–V25, but content equivalence and checksum validation are outstanding.

These are static observations, not PostgreSQL 16, Flyway migrate/validate, EXPLAIN, rollback or integrity test results. Do not merge while conflicts remain.
