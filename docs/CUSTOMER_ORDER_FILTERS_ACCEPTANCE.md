# Customer order filtering — acceptance contract

Status: PROPOSED / NOT IMPLEMENTED / NOT ACCEPTED. Related to roadmap #019 and #022, issue #3.

## Functional contract
- An authenticated customer may filter only their own orders by exact order identifier and allowlisted order status; invalid values return a controlled validation error.
- Use bounded pagination with deterministic ordering; enforce maximum page size and reject negative indexes.
- Do not accept user IDs or tenant IDs as client-controlled authorization selectors. Apply ownership restrictions at repository/query level before pagination and counts.
- Do not expose another customer's order existence through detail, search, totals, or error messages.
- Admin/staff views retain their separate, explicit role and assignment policies; customer filters must not expand their privileges.

## Required tests before implementation is marked done
1. Customer A sees their matching orders, correct page totals, and stable ordering.
2. Customer A cannot find Customer B's orders by exact ID, status, pagination, or crafted query parameters.
3. Anonymous access is rejected; staff/admin privileges remain unchanged.
4. Unknown statuses, malformed IDs, oversized/negative pages, and empty filters are handled predictably.
5. CSRF is preserved for any state-changing action; GET filtering has no side effects.
6. PostgreSQL integration tests verify query scoping and pagination, including concurrent changes.
7. Java/PostgreSQL/Flyway CI, CodeQL findings, JaCoCo, and manual mobile UAT are reviewed with linked run evidence.

## Owner sign-off
- Review API response semantics, UI labels, maximum page size, staging UAT and rollback plan.
- No production integration or real social-media engagement automation is authorized by this document.
