# Customer notification inbox (prototype)

GET `/notifications?page=0&unreadOnly=false`:
- Requires a session; `unreadOnly=true` filters the signed-in user's unread notifications only.
- Page bounds are 0..10000, fixed size 20, stable order `createdAt DESC, id DESC`.
- A page query is applied inside the database; no unbounded account list is loaded.
- Owner-scoped unread count and browser `Cache-Control: no-store`.

POST `/notifications/{id}/read`: only the owner may read their entry, otherwise 404.
POST `/notifications/read-all`: marks only the signed-in customer's currently unread entries read. An atomic database update keeps prior read timestamps intact and is safe to repeat. Both POST endpoints require the existing CSRF token.

Tests: `mvn -Dtest=NotificationsIntegrationTest,NotificationInboxIntegrationTest test`, followed by `mvn test`.
No production deployment or roadmap acceptance without CI and staging UAT.
