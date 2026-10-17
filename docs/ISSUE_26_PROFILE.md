# Customer-only contact profile (Issue #26)

The established `/profile` screen continues to show username, email and role as **read-only** identity data. New `/profile/edit` form (CSRF-protected) allows updates to display name (2–80 characters) and optional phone, never passwords, email, roles, or account status.

`GET /api/v1/customer/profile` and `PUT /api/v1/customer/profile` accept no owner selector; the principal is the only identity. `PUT` JSON accepts `displayName` and `phone`. Response excludes password and secrets; `Cache-Control: no-store`. The optional `customer_profiles` table is isolated from `users` and created by additive Flyway V12.

Tests: separate owners, forged role/email/username fields, missing CSRF, invalid input, anonymous denial, existing account fields unchanged. This does **not** implement secure email or password change without re-verification.
