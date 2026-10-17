# W4 Staff directory (issue #28)

**State:** Draft feature, security/UAT acceptance still pending.

- `GET /admin/staff?role=ALL|STAFF|ADMIN&username=<exact>&page=0` reads all active and locked STAFF/ADMIN accounts, never USER accounts.
- Bounded 20/page+1 with deterministic username/id ordering, not an unbounded staff dropdown.
- A single grouped database query counts approved/in-progress orders for only the visible staff; avoids N+1 workload queries.
- No password/email/IP/device data shown; read-only. For lock or role change, use the existing Admin account page, its CSRF and transactional audit.
- Tests include exact search, role/disabled state, scoped active order counts, page limits, invalid input and STAFF/USER/anonymous denial.
- No new migration, no production deployment, no wage/commission/revenue assumptions.
- Human UAT needed for responsive staff directory and performance with realistic rows; benchmark before claiming optimization.
