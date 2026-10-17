# Chat read receipts and reply notifications — Issue #27

Existing PR #23 already provides paginated owner-scoped threads.
This change adds a nullable `chat_messages.read_at` (Flyway V12) and index
`(receiver,sender,read_at)` without changing existing thread pagination.
POST `/chat/thread/{other}/read` requires session + CSRF and atomically marks only
messages addressed to the signed-in principal as read; it is idempotent.
Thread JSON includes nullable `readAt` for each message.
Sending a message to a currently registered recipient creates a website notification;
the notification never embeds untrusted message body. No external email/SMS.
Recipient existence is not disclosed via new API responses.
No message deletion, real-time push or automated polling is promised.
Security tests cover forged sender, foreign reader, CSRF, duplicate mark, and XSS payload.
Human review, PostgreSQL migration and browser UAT are needed before acceptance.
