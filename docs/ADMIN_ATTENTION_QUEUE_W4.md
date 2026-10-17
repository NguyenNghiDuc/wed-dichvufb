# W4: Admin priority review queues (Issue #28)

Status: draft, awaiting CI/security findings and owner sign-off.

Read-only endpoint `GET /admin/attention?queue=OVERDUE_PENDING|OVERDUE_IN_PROGRESS|READY_TO_ASSIGN|REJECTED&page=N` (Admin only).
- `OVERDUE_PENDING`: PENDING_REVIEW older than 48h by creation timestamp.
- `OVERDUE_IN_PROGRESS`: IN_PROGRESS older than 7 days since last update.
- `READY_TO_ASSIGN`: approved orders with no staff assignee.
- `REJECTED`: rejected orders for manual review (does not imply payment failures).
- Queries are bounded (20 rows/page plus one for next page), filtered **in database** with stable sorting and page <= 10000. No unbounded Java filtering.
- Links open the exact campaign in existing Admin filtered list, where mutations keep their current CSRF, transactional locking and campaign event audit.
- No state mutations in the queue, no service-level guarantee, no payment/refund/notification actions.
- Regression H2/MockMvc tests cover correct queues, fresh versus aging data, page boundary and USER/STAFF denial. PostgreSQL/Flyway/JAR/Docker CI and CodeQL logs must be confirmed on latest head.
- Owner must approve 48h/7day thresholds, time zone policy and test realistic performance; roadmap #039/#082 remain unchecked pending acceptance.
