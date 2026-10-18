# W4 — Admin account overview (Issue #28)

Status: draft implementation, not security or owner accepted.

The read-only `GET /admin/users/{id}?page=N` shows ID, username, role, enabled flag and 20 latest audit records for the requested account. It reuses `AdminUserService.pageAudit` (exact positive target ID, SQL-level paging) and displays only a DTO **without password hashes or email/IP/contact details**.

Users need ADMIN permission to view this route; STAFF, USER and anonymous requests are denied. Invalid IDs and pages fail closed; unknown account returns HTTP 404. Linking to the existing Admin user search is used for edits, retaining its CSRF/audit/transaction safeguards.

MockMvc tests verify privacy, ownership of audit events, paging limits and access controls. Not a compliance-grade immutable log. Real browser UAT, XSS review, authorization review, audit retention and CodeQL CSRF issue #13 need signoff. No production deploy.
