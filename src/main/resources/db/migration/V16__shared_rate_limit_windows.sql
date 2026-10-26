-- Shared, atomic abuse counters. Bucket identifiers contain hashes, not raw personal data.
CREATE TABLE rate_limit_windows (
 bucket_hash VARCHAR(64) PRIMARY KEY,
 expires_at_ms BIGINT NOT NULL,
 hits INTEGER NOT NULL CHECK (hits > 0)
);
CREATE INDEX idx_rate_limit_expiry ON rate_limit_windows(expires_at_ms);
