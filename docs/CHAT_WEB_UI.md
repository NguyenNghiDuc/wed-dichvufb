# GrowLab chat UI — Issue #27

GET `/chat/ui?page=0&q=name`: authenticated, owner-only conversation index, 20 distinct contacts/page, optional literal username search and scoped unread badges. Also shows the staff directory (enabled accounts), not real-time online presence.
GET `/chat/contacts?page=0&q=name`: JSON Page of `{contact, unreadCount}` (20/page), scoped to authenticated Principal, `Cache-Control: no-store`.
GET `/chat/ui`: authenticated customer or staff contacts page with at most 100 enabled
STAFF accounts. GET `/chat/ui/open?contact=username`: validate contact and redirect.
GET `/chat/ui/{other}?page=0`: render only the authenticated viewer's
two-way thread with that contact, 20 messages per page newest-first, max page 10000.
This reuses merged PR #23 pagination rather than reimplementing the query.
POST `/chat/ui/{other}/send`: write only as session Principal; 1..255 chars,
CSRF mandatory. POST `/chat/ui/{other}/read`: mark only incoming messages read,
CSRF mandatory. Missing users return 404. No websocket/live typing claimed.
Render all untrusted message content with escaped Thymeleaf `th:text` (not `th:utext`).
White-orange responsive cards and mobile messages CSS.
Tests `ChatWebIntegrationTest` cover pagination, cross-account isolation, XSS,
forged sender, CSRF and anonymous access.
Tests also include `ChatConversationIndexIntegrationTest`, `ChatUnreadCountsIntegrationTest`, `ChatContactsApiIntegrationTest` and PostgreSQL query checks.
Do not merge until Java CI, PostgreSQL/Flyway migrations V10–V15, CodeQL and
mobile/staging UAT pass. No auth/payment/order code altered.

## Browser chat loading failures (W3)

The chat-only `ChatUiDataErrorHandler` catches repository `DataAccessException` without showing connection strings, internal errors, usernames or message bodies. `/chat/ui` and thread browser views return HTTP **503** with `Cache-Control: no-store` and a responsive, script-free `chat_error.html` fallback containing retry/support navigation. Invalid search/page input still returns 400; anonymous access still redirects. `ChatUiDataErrorIntegrationTest` injects a database failure and checks HTTP status, safe content, redaction and auth/validation behavior. Does not catch errors in unrelated W1/W6/W8 controllers.
