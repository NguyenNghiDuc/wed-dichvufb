-- Accelerate chronological ticket thread pagination without changing existing data.
CREATE INDEX idx_support_msg_timeline ON support_messages(ticket_id, created_at, id);
