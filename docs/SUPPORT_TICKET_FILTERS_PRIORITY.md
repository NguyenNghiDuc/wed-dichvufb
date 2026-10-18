# Support ticket search and priority (#27)

Ticket priority is staff-controlled and distinct from the status workflow:
LOW, NORMAL (default), HIGH, URGENT. PostgreSQL Flyway V11 adds the column and
a supporting queue index, preserving existing ticket values.

Customer `GET /support?subject=...&status=OPEN&page=0` queries exclusively their own rows.
Staff `GET /admin/support?subject=...&status=OPEN&priority=URGENT&assignment=MINE&page=0`
uses database-side predicates and stable 20-record pagination.
Subject is a bounded, case-insensitive literal substring; LIKE wildcards are escaped.
Status, priority and assignment are enums; `assignment=ALL|MINE|UNASSIGNED` never accepts an arbitrary assignee username. Invalid values return 400. Invalid page returns 400.

STAFF and ADMIN only may `POST /admin/support/{id}/priority` with CSRF.
Closed tickets cannot be reprioritized. No payment/order/auth code changed.

Testing: `SupportTicketSearchPriorityIntegrationTest` plus existing support tests,
Flyway PostgreSQL V1–V11 migration CI, manual staff/customer review.
These are prototypes until CI, staging UAT, security review and owner sign-off.
