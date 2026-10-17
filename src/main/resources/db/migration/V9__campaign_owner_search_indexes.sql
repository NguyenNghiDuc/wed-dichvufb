CREATE INDEX idx_campaign_owner_status_created ON campaign_orders(owner_username,status,created_at DESC);
CREATE INDEX idx_campaign_owner_platform_created ON campaign_orders(owner_username,platform,created_at DESC);
