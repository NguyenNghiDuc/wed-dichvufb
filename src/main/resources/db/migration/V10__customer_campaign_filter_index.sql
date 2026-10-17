-- Customer-scoped, stable timeline paging and date filters.
-- Additive index; no historical prices/statuses are changed.
CREATE INDEX idx_campaign_owner_created_id
    ON campaign_orders (owner_username, created_at DESC, id DESC);
