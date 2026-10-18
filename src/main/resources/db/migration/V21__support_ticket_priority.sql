-- Existing support tickets retain a neutral default; no destructive migration.
ALTER TABLE support_tickets ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL';
CREATE INDEX idx_support_staff_queue ON support_tickets(status, priority, updated_at DESC, id DESC);
