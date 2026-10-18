# W3 · Ticket reopening, status timeline and progress (Issue #27)

This work extends existing Draft PR #35 on `feature/issue27-support-workflow`.
**No Flyway migration, core login code, payment code or W8 notification code is changed.**

## Owner-only reopen

- `POST /support/{id}/reopen` requires the normal authenticated browser
  session and Spring Security CSRF token.
- Only the exact owner from `principal.getName()` can reopen a ticket;
  the service obtains a pessimistic lock with both `ticket.id` and
  `owner_username` in the query. A foreign ID is 404, not a disclosure.
- Only `CLOSED -> OPEN` is allowed; reopening an already OPEN/ANSWERED ticket
  is a validation error. Reason: 10–500 characters, shown using escaped
  Thymeleaf `th:text` and stored in that same ticket's private reply history.
- Reopen requests are limited to 5 per owner per 24-hour window using the
  existing `TrafficLimiter` (HTTP 429 when the limit is reached).
- The transition, a system-authored lifecycle event and the owner's reason
  are persisted in one transaction. A duplicate/concurrent reopen cannot
  append a second set of events. The existing STAFF/ADMIN close action also
  appends a system-authored `CLOSED` entry to the same bounded timeline.
- A reopened ticket retains its previous assignee; authorized ADMIN may
  reassign it using the existing audited mechanism. Reopening never
  revives a previously disabled account or sends a new notification.

## Optional staff closing note

- STAFF/ADMIN may optionally send `resolutionNote` (10–500 characters
  when provided) with `POST /admin/support/{id}/close`. Omission is
  backward-compatible with existing close actions. The existing
  assignee/role authorization and CSRF rules still apply.
- A supplied note is written as a private ticket reply, together with the
  system-authored CLOSED status entry, in the same transaction.
  It is never copied into W8 notifications, does not change the database
  schema, and is escaped by `th:text` for both customer/staff views.
- `SupportTicketClosureNoteIntegrationTest` checks normal CSRF,
  authorization, input bounds, timeline persistence, foreign-owner access
  restrictions, and stored XSS. This is an internal note, not an SLA
  resolution timestamp.

## Owner- and staff-scoped progress

- `GET /support/{id}/progress` requires the exact ticket owner;
  `GET /admin/support/{id}/progress` uses the existing STAFF/ADMIN URL gate.
  Both responses are read-only with `Cache-Control: no-store`.
- The progress snapshot exposes status, priority, whether a staff member
  is assigned, server-local `createdAt`, `lastActivityAt`, elapsed
  `ageMinutes` and `idleMinutes`. It contains no password, customer email,
  reply body, or unrelated ticket data.
- Customer and staff ticket detail pages show a responsive six-field
  progress panel. Empty support reply history has an accessible status message.
- These durations measure creation/last **activity** only. They are **not**
  authoritative first-response time, closure time, resolution time or a
  promised SLA. Accurate historical SLA metrics would require an approved
  migration/data model owned by W7; this branch does not invent one.

## Validation scope

- `SupportTicketReopenIntegrationTest`: CSRF, closed-only policy, exact
  ticket ownership, validation, XSS escaping, close/reopen lifecycle messages,
  idempotent repeat behavior.
- `PostgresSupportReopenConcurrencyTest`: simultaneous PostgreSQL attempts
  to reopen one CLOSED ticket must produce one success and three timeline
  rows (close event, reopen event, user-provided reason).
- `SupportTicketProgressIntegrationTest`: owner vs unrelated customer,
  STAFF/ADMIN route, anonymous rejection, no-store, nonnegative durations,
  rendered HTML and an empty-history state.

**Release gates remain open:** Java 17 / PostgreSQL CI on the exact HEAD,
CodeQL and findings review, manual responsive/keyboard UAT, test on a
combined Flyway migration chain coordinated by W7, and human approval.
PR #35 stays Draft; no deployment or merge is implied.

## Duplicate FAQ candidate decision

PR #42 and PR #43 both implement public FAQ search and modify
`HelpController.java` plus `help.html`. On the inspected heads, Java CI
and CodeQL workflows for both are successful. **Prefer PR #43 as the sole FAQ
integration candidate** because it has 14 curated answers, category filters,
accent-insensitive AND-term search and native accessible help toggles.
Keep #42 unmerged as a competing draft and do not copy either FAQ
implementation into the W3 chat/support branch. Any future integration
requires conflict checks, a fresh merged-tree CI run and review.
