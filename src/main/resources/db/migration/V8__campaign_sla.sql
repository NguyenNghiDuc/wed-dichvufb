ALTER TABLE campaign_orders ADD COLUMN due_at TIMESTAMP(6);
ALTER TABLE campaign_orders ADD COLUMN overdue_notified_at TIMESTAMP(6);
UPDATE campaign_orders SET due_at = CASE
    WHEN status = 'PENDING_REVIEW' THEN created_at + INTERVAL '24 hours'
    WHEN status IN ('APPROVED','IN_PROGRESS') THEN updated_at + INTERVAL '72 hours'
    ELSE updated_at END WHERE due_at IS NULL;
CREATE INDEX idx_campaign_sla_due ON campaign_orders(status,due_at);
CREATE INDEX idx_campaign_sla_alert ON campaign_orders(overdue_notified_at,due_at);
