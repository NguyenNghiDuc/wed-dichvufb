# Support ticket claiming workflow — Issue #27

STAFF/ADMIN can POST `/admin/support/{id}/claim` with CSRF to take an open
unassigned ticket. The service revalidates that the claimant is an enabled STAFF/ADMIN
account in the database, and locks the ticket row to prevent double claiming.
Repeated claim by the same staff is idempotent; claim by someone else is rejected.
Once claimed, another staff member cannot reply or close it; the claim owner may.
The current rule intentionally has no reassignment/admin override; a supervised
transfer workflow and absence coverage are future tasks.

PostgreSQL V13 adds nullable `assignee_username` and index. Existing tickets
remain unassigned. Successful claim sends one website notification to owner;
no tokens or external calls. No login, payment or order code is modified.
Tests: `SupportTicketClaimIntegrationTest` for CSRF, user role, ownership,
idempotence, contention, reply and close boundaries. PostgreSQL, CodeQL,
race testing and staging UAT still required before merge/production.
