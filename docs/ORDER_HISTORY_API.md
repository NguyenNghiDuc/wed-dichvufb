# Campaign history: bounded owner-scoped read

## API
`GET /api/v1/campaign-orders/{id}/history?page=0&size=20`

- Requires an authenticated customer; user identity comes from the session.
- The campaign ID must belong to that customer, otherwise returns HTTP 404 (same response as unknown ID).
- Page: 0–10000, size: 1–50; invalid values return 400.
- Results are ordered by `createdAt ASC, id ASC` with a stable tiebreaker.
- JSON returns `oldStatus`, `newStatus`, `description`, `createdAt`; internal actor usernames are not returned.
- The HTML details page at `/campaigns/{id}` also reads a 20-event page and provides previous/next links.
- Historical entries are read-only; they do not trigger billing, real delivery, or social platform API actions.

## Acceptance
Verify owner isolation against two accounts, status transitions, empty/short pages, forged campaign ID, anonymous redirect, invalid sizes, and a mobile browser. Integration tests cover API access, data boundaries and HTML rendering. CI success does not replace human staging validation.
