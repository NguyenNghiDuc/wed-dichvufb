# Issue #26 — lifecycle, pricing and tenancy regression

This test-only hardening checkpoint verifies:
1. A customer JSON request with forged owner, quote, status and assignee cannot set those fields; quote is always derived from the server's active catalog.
2. Customer cannot access Admin status-changing endpoints.
3. Invalid external URL is rejected, and state-mutating requests require CSRF.
4. Owner may cancel pending review only once; approved campaigns cannot be cancelled by customer.
5. Different owner cannot cancel, read, or inspect event history.
6. Illegal jumps from APPROVED directly to COMPLETED are rejected.

Tests use fake data in an H2 test database, not real payment/account operations. Production acceptance requires Java/PostgreSQL CI, CodeQL and manual staging reviews.
