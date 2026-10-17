-- Additive indexes for owner-scoped deposit history and UTC-oriented settlement review.
-- Existing uniqueness for event_id and deposit_request_id remains in V3.
CREATE INDEX idx_deposit_owner_created ON deposit_request (owner_username, created_at DESC);
CREATE INDEX idx_payment_receipts_created_at ON payment_receipts (created_at);
