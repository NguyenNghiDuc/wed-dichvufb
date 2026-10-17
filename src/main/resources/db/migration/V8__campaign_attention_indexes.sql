-- Read-only operational review metrics for pending and in-progress campaigns.
-- PostgreSQL migration V8. Review query plans and backup procedures before deployment.
CREATE INDEX idx_campaign_status_created ON campaign_orders(status, created_at);
CREATE INDEX idx_campaign_status_updated ON campaign_orders(status, updated_at);
