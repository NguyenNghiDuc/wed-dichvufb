# Support ticket handoff and assignment audit — Issue #27

Admin-only `POST /admin/support/{ticketId}/reassign` with CSRF and optional
`employeeId`; empty employeeId means unassign. The service checks that the actor
is an enabled ADMIN in database, validates enabled STAFF/ADMIN recipients, locks
the ticket row, and forbids duplicate or closed-ticket changes. STAFF can claim
unassigned tickets, but cannot override someone else's claim.

New PostgreSQL Flyway V14 `support_assignment_events` stores an append-only
audit of both first claims and reassignments, including actor and old/new staff.
Admin/staff detail shows at most the latest 50 entries, backed by an index.
The customer receives a generic reassignment notification (no personal staff data).
New staff receives a notification without the customer's private ticket contents.

CI H2/PostgreSQL, CodeQL, CSRF/access denial, duplicate transfer and
browser acceptance still required; not a production merge yet.
