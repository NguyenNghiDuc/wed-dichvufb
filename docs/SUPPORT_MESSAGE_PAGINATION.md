# Support ticket replies — bounded timeline (#27)

GET `/support/{id}?page=0`: customer owner only.
GET `/admin/support/{id}?page=0`: STAFF/ADMIN only (existing route authorization).
Both display at most 20 replies using a deterministic database sort `createdAt ASC, id ASC`;
page is 0..10000 or returns HTTP 400. Unknown/foreign tickets return HTTP 404.
CSRF remains required for replies. Text is rendered with Thymeleaf `th:text`, not `th:utext`.
PostgreSQL Flyway V20 adds a non-destructive `(ticket_id, created_at, id)` index.
Existing unbounded list methods are retained only for backwards compatibility; no longer used by detail pages.
Integration tests: `SupportMessagePaginationIntegrationTest`, `SupportIntegrationTest`.
Staging PostgreSQL, CodeQL, browser UAT and review are still acceptance gates.
