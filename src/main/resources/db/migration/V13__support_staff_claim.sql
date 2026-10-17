-- Nullable assignment keeps preexisting tickets unassigned; no data is lost.
ALTER TABLE support_tickets ADD COLUMN assignee_username VARCHAR(100);
CREATE INDEX idx_support_assignee ON support_tickets(assignee_username, status, updated_at DESC);
