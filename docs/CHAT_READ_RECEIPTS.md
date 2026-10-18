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

## W3 incremental improvements, 18 October — per-message acknowledgment

- JSON API `POST /chat/messages/{id}/read` updates exactly one incoming row
  using `WHERE id = :messageId AND receiver = :authenticatedViewer AND read_at IS NULL`.
  Returns `{"marked":1}` on first acknowledgment, otherwise `{"marked":0}`
  for previously read, missing, or another account's message. All POSTs
  require the normal session and CSRF token. Invalid nonpositive IDs return 400.
  No message content or foreign contact information is returned.
- Browser chat `/chat/ui/{contact}` displays a CSRF-protected action **only**
  for each incoming unread message. The server form route uses the stricter
  predicate `id + receiver + sender/contact`, preventing forged forms from
  acknowledging a message in another chat. Existing mark-entire-thread
  behavior remains; no polling or auto-mark-on-view.
- JSON `POST /chat/send` accepts only an enabled existing account with
  username `[a-zA-Z0-9._-]{3,40}`, disallows self-addressing and rejects
  unknown/disabled destinations with 404. The sender, ID and read status are
  never taken from user-supplied JSON. Payload validation and CSRF run before
  any message persistence. Existing notification implementation is untouched.
- `ChatIndividualReadReceiptIntegrationTest`,
  `ChatRecipientValidationIntegrationTest` and updated web/CSRF tests
  cover outsiders, sender spoofing, invalid targets, idempotency and XSS
  escaping. `PostgresChatIndividualReadConcurrencyTest` checks simultaneous
  acknowledgments produce one update and validates the preexisting
  `idx_chat_received_unread` migration index on PostgreSQL. No migration
  SQL has been modified by this increment.
- Outstanding: exact-head CI and CodeQL, mobile and accessibility walkthrough,
  production-size PostgreSQL performance review and independent security QA.
