# Content draft editing and pagination — W5 / issue #29

Authentication is required for all routes. Existing POST /content-calendar still creates a draft.

- GET /content-calendar?page=0 — newest-first, page size 20, valid pages 0–10000; only owner-owned rows from the database.
- GET /content-calendar/{id}/edit — edit form for the authenticated owner.
- POST /content-calendar/{id}/edit — validate and update title, body, platform and optional UTC scheduledAtUtc.
- POST /content-calendar/{id}/delete — remove a draft; CSRF protection required.
- Absent/empty scheduledAtUtc moves PLANNED back to DRAFT. Setting a UTC time changes to PLANNED, but **does not publish any content**.
- Accessing another customer's or a nonexistent draft returns 404. There is no client-provided owner or mass-update endpoint.
- Update and delete use owner-bound SQL row locks inside transactions to reduce concurrent-write races.
- LocalTime input is explicitly UTC; dates must be at least one minute and at most one year ahead.
- Thymeleaf renders user text with escaped th:text/th:value (no th:utext).
- Test: `mvn -Dtest=ContentDraftManagementIntegrationTest test` and full `mvn clean verify` with PostgreSQL in CI.

Still required for production acceptance: owner UAT, mobile layout checks, and review of extreme-volume pagination.
