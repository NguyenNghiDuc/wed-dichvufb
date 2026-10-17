-- V9: bounded and deterministic two-party messaging queries.
-- No existing messages or legacy endpoints are removed.
CREATE INDEX idx_chat_sender_receiver_time
    ON chat_messages (sender, receiver, "timestamp" DESC, id DESC);
CREATE INDEX idx_chat_receiver_time
    ON chat_messages (receiver, "timestamp" DESC, id DESC);
