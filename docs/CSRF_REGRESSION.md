# CSRF boundary regression checklist — roadmap #097 / issue #3

**Status:** Code and tests added, security review and staging acceptance pending.

## Threat model and small hardening change
- Login and registration GET forms are public but provide explicit hidden CSRF tokens; POSTs must present valid CSRF.
- Owner/admin mutations and logout remain CSRF-protected.
- The only CSRF exception is the HMAC-authenticated internal webhook `POST /api/v1/payments/webhooks/internal`; other unsafe HTTP methods at the same URL are not exempt.
- The internal webhook does not bypass its HMAC signature validation, even when CSRF is exempt. Tests use a fake local test string only; no financial transfers occur.
- These tests cover selected attack paths, **not all XSS, SSRF, session or dependency risks**.

## Required evidence / follow-up
- Java CI (H2 security tests and PostgreSQL/Flyway/JAR/Docker), CodeQL and JaCoCo results at latest head.
- Validate reverse proxy TLS/secure-cookie/CSRF integration in staging using real browser sessions.
- CodeQL's `java/csrf-unprotected-request-type` has emitted SARIF path graph processing errors involving GET handlers `loginPage` and `registerPage` despite job success; changing CSRF scope does not establish that this exporter issue is fixed. Requires independent triage of alerts and potential analyzer defect.
- Owner security review before any merge or production use. Roadmap #097 remains unchecked.
