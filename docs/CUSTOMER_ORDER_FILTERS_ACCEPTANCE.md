# Customer order filtering — acceptance contract

Status: IMPLEMENTED PROTOTYPE / NOT SECURITY-ACCEPTED / NOT UAT-ACCEPTED. Related to roadmap #019 and #022, issue #3.

## Implementation (draft PR #6)
- `CampaignOrderService.pageMine(..., orderId, status)`: typed database Criteria predicates **always** apply owner scope to both results and counts; bounded page and size.
- `GET /api/v1/campaign-orders/page?orderId=<positive integer>&status=<enum>` (both optional). The unchanged `GET /api/v1/campaign-orders` remains a backwards-compatible bounded list.
- `GET /campaigns`: customer search form and preserved filters in pagination links.
- MockMvc H2 regression tests in `CampaignOrderFilteringIntegrationTest` plus real PostgreSQL query test in `PostgresCampaignFilteringIntegrationTest`.
- Java CI run 39034675236 (commit a4b1d51): **44 tests, no failures**, Flyway v7, production JAR and Docker build passed.
- CodeQL run 39034675155: job succeeded, **but rule `java/csrf-unprotected-request-type` raised SARIF graph-processing errors for `registerPage` and `loginPage`**. Security triage is still required. Newer Actions workflow at commit `fbc4294` is being verified independently.

## Functional contract
- Authenticated users filter only their own orders by exact order identifier and allowlisted status.
- Invalid values produce a controlled 400 response. Pagination is deterministic and bounded.
- User IDs or tenant IDs are never accepted as client-controlled authorization selectors.
- Both page results and aggregate counts enforce ownership before returning to the client.
- Admin and staff authorization boundaries are unchanged.

## Acceptance gaps and owner sign-off
- Test concurrent updates while paginating and confirm stable client pagination behavior.
- Security review of CodeQL findings, including the reported path graph processing errors.
- Review JaCoCo coverage, human code review, staging mobile/browser UAT, accessibility, operational rollback and backups.
- Review API parameter naming and UI labels with the owner. Do not mark roadmap #019/#022 as completed yet.
- No production integrations, real financial transactions or artificial social-media engagement are authorized.
