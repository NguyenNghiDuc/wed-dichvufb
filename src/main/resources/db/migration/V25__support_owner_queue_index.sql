-- Supporting index for owner-scoped support search and stable list pagination.
CREATE INDEX idx_support_owner_queue
 ON support_tickets(owner_username, status, updated_at DESC, id DESC);
