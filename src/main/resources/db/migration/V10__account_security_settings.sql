-- Existing accounts and sessions start at revision zero. Incrementing auth_version
-- revokes every earlier database-authenticated session on its next request.
ALTER TABLE users ADD COLUMN display_name VARCHAR(80);
ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0;
