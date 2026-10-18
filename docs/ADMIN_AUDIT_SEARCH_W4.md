# W4 audit search acceptance (Issue #28)

**Implementation stage only; not production/security accepted.**

- `/admin/users/audit` now supports exact Admin actor, positive account ID, and allowlisted `ACCOUNT_ROLE` or `ACCOUNT_ENABLED` type, with 20-entry database paging.
- The history remains read-only and ADMIN-scoped; STAFF, USER and anonymous requests are blocked.
- Invalid page / actor / target / operation return HTTP 400. Pagination preserves the selected filters. H2/MockMvc and PostgreSQL regression tests cover authorization and SQL-side isolation.
- Catalog price audit is available at `/admin/catalog/history` with exact service and actor search, 20-entry database paging and a link from the catalog editor.
- This activity history is held in the normal database, not an immutable compliance log. Security review (including CodeQL issue #13), retention/backups, browser UAT and realistic performance review are still required before closing roadmap #090.
