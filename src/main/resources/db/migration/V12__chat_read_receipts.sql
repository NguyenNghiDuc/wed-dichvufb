-- Backward compatible read-at tracking; existing messages remain unread by default.
ALTER TABLE chat_messages ADD COLUMN read_at TIMESTAMP(6);
CREATE INDEX idx_chat_received_unread ON chat_messages(receiver, sender, read_at);
