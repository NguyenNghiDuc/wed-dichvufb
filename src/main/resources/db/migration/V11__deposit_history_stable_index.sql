-- Replace the V10 owner/time index with one matching deterministic paging
-- ORDER BY created_at DESC, id DESC. The new index also supports owner/time
-- queries via its leftmost prefix; do not modify applied V10 migration.
CREATE INDEX idx_deposit_owner_created_id
    ON deposit_request(owner_username, created_at DESC, id DESC);
DROP INDEX idx_deposit_owner_created;
