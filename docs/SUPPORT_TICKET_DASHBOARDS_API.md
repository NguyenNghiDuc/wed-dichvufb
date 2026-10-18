# Issue #27 — ticket activity, summaries and paginated reply APIs

## Recent activity sorting

Every valid customer/staff reply advances `support_tickets.updated_at` even if
status stays unchanged (OPEN or ANSWERED). This keeps recently active tickets on
top of existing descending `updatedAt, id` list queries. A no-op lifecycle
transition does not by itself trigger JPA `@PreUpdate`; explicit activity
updates fix that gap. Tested by `SupportTicketActivityIntegrationTest`.

## Dashboard summary (no database migration)

- `GET /support/stats` — authenticated customer, counts only the signed-in
  customer's OPEN/ANSWERED/CLOSED tickets.
- `GET /admin/support/stats` — STAFF/ADMIN role, counts queue-wide tickets.
- Response fields: `open`, `answered`, `closed`.
- Counts use SQL aggregates, not unbounded list retrieval.
- Both HTML ticket list pages show state counters linking to status filters.

## Ticket reply pages for mobile clients

- `GET /support/{id}/messages?page=0` — ticket owner only.
- `GET /admin/support/{id}/messages?page=0` — STAFF/ADMIN only.
- Exactly 20 replies per database page, ordered `createdAt ASC, id ASC`.
- `page` must be 0..10000; bad values return HTTP 400.
- Foreign/unknown customer ticket returns HTTP 404, without disclosing counts.
- Response is a Spring Page of `SupportMessageEntry`:
  `id, author, body, createdAt`. No entity or internal fields are serialized.
- HTTP responses set `Cache-Control: no-store`; GET is read-only.
- JSON string values, including HTML-looking customer input, remain data.
  Clients must render them as text, not unsanitized HTML.

## Verification and release gates

`SupportTicketActivityIntegrationTest`,
`SupportTicketCountsIntegrationTest`,
`SupportTicketMessagesApiIntegrationTest`, prior support and chat tests,
H2 + production PostgreSQL CI, CodeQL and mobile browser UAT.
Review Flyway collision policy when merging the Issue #27 feature branch
into a moving main; this change itself adds no new migration.
Do not ship without full HEAD CI and security review.
