# Issue #28 — Admin role distribution

Dashboard `GET /admin/dashboard` lists grouped USER/STAFF/ADMIN account counts with total, enabled and locked numbers using one typed JPQL aggregate over `User`; no individual email, password, token, IP or user profile is exported.

Read-only `GET /admin/reports/overview.csv` adds a `role,total_users,enabled_users,locked_users` section. The report remains no-store and does not represent active web sessions or a count of *new* signups, because the current User schema does not record createdAt.

MockMvc tests verify grouped counts, DB flags, HTML and CSV privacy, and ADMIN-only access versus USER/STAFF/anonymous. It does not modify WebSecurityConfig, account enable/role operations, payment, CI, Flyway, or login information.
