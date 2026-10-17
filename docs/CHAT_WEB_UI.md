# GrowLab chat UI — Issue #27

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
Do not merge until Java CI, PostgreSQL/Flyway migrations V10–V12, CodeQL and
mobile/staging UAT pass. No auth/payment/order code altered.
