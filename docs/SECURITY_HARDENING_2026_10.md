# Security hardening — October 2026

This change addresses the first operational issues in the supplied review. It is not a completed security audit or a production certification.

## Implemented behavior

- `TrafficLimiter` uses an atomic database counter shared by every instance connected to the same database. Its short transactions commit separately from business transactions so rolling back an invalid request cannot reset the abuse budget. Only SHA-256 bucket identifiers are stored. Expiration belongs to each stored window; a caller with a shorter interval cannot expire another budget. Expired windows are removed on new-bucket creation; the approximate capacity bound is 20,000 windows, with concurrent insertion allowed to exceed it temporarily by the number of concurrent creators.
- Authentication applies a source/account budget (10 attempts per 10 minutes) and a broader source budget (300 login attempts or 30 registrations per 10 minutes). Users behind one proxy do not share the small 10-attempt account budget. These budgets complement, rather than replace, edge protection against volumetric abuse.
- Five failed attempts block that account/source pair for 15 minutes. A correct login clears only that pair's failure counter. Another source can still authenticate; already valid sessions remain valid. Legacy account-level failure fields are retained as diagnostic counters, not a global blocking decision. There is no automatic bypass of password verification or CSRF.
- An explicit authentication manager has no parent fallback, preventing one failed login from invoking the guarded provider twice.
- Deposits allow 100 attempts per hour and at most 25 pending requests per account. Locking the user row serializes the pending-count check across instances. Real enabled owners are required. The request form no longer accepts or persists bank names/account numbers; historical bank data is preserved and still requires a separately planned encryption/retention migration.
- Campaign creation: 100 requests/hour/account. Content creation/edit/delete: 200 writes/hour/account. Chat send: 60/minute/account, only enabled existing recipients, with at least one staff/admin participant. Customer-to-customer and self messages are rejected. Historic threads remain visible only to their participants. HTTP chat responses use explicit DTOs; supplied sender/id are ignored.
- Account status and tracked-session revocation remain checked on each authenticated request. Session refresh writes are throttled to 60 seconds (or one quarter of a shorter idle timeout); refresh uses an update conditional on the session still being active. Recorded idle expiration can precede the servlet session's expiration by at most that interval. No account status cache is introduced.
- Internal payment webhook reads no more than 4097 bytes and rejects payloads over 4096 bytes before signature/JSON processing, including unknown-length bodies. This is not a real payment provider integration, replay-timestamp protocol or wallet credit implementation.

## Trusted reverse proxy configuration

Default: `FORWARD_HEADERS_STRATEGY=none`, no forwarded IP headers trusted. Do not turn on framework forwarding, which has no peer allowlist in this configuration.

When an operator has established the actual trusted proxy peer addresses and the edge strips/replaces client-supplied forwarding headers, configure:

```dotenv
FORWARD_HEADERS_STRATEGY=native
# Example ONLY: replace these addresses with your actual edge peers.
TRUSTED_PROXY_REGEX=^(10[.]0[.]0[.]10|10[.]0[.]0[.]11)$
```

Startup rejects native mode with an empty/unrestricted common wildcard allowlist, and rejects `framework`. Do not include arbitrary private networks or `.*`. Tomcat's RemoteIpValve processes the forwarded chain before servlet filters only when the direct peer is trusted. No actual deployment proxy addresses have been inferred or installed by this change.

Use V16 before deploying new application code on PostgreSQL. H2 development creates the corresponding mapped table; do not run production against H2. All application instances must use the same database. Plan database sizing/connection pools: each limiter operation needs an independent transaction; a pool must have spare connections beyond simultaneous outer business transactions. Redis/edge rate limiting may be preferable for a high-volume deployment. Lower-volume database limits are not a load-test result.

## Still outstanding from the supplied review

- Supported Spring Boot/dependency upgrade and profile separation for H2 need a dedicated compatibility change.
- Email verification, reset, 2FA, official OAuth require provider configuration and a separate security design.
- Existing bank data encryption/retention and access policies need a data migration/key-management plan; existing values have not been removed.
- A signed webhook timestamp/provider-specific validation and an actual wallet ledger remain unimplemented. An APPROVED legacy deposit does not credit a balance.
- Case-insensitive account identity migration must resolve collisions before adding uniqueness; no existing usernames/password hashes are rewritten.
- Product direction was confirmed by the owner: new orders now use consulting/content/advertising strategy. Legacy HTTP create endpoints return 410; historical data and internal legacy services remain for archival compatibility. No external fulfillment/provider integration has been added.
- Tracked target artifacts, unused pom backup and the duplicate `src/main/ resources` tree have been removed from Git. The misplaced repository test has moved to the actual Java test tree with an isolated in-memory database. Workflow actions are pinned to the dereferenced commit of their existing major tag, resolved from the official repositories. Coverage policy, database backup/restore and production load testing remain separate tasks.

Java 17 enforcement, CSRF protection, TLS verification and authentication/session permission checks remain enabled.

## Validation performed

- 8 Python Flyway guard tests and collision check: pass, 16 migrations.
- `mvn clean verify`: 173 Java tests, zero failures/errors/skips, including PostgreSQL migration, concurrency, shared-rate-window and pending-deposit-cap tests. JaCoCo report generated in the isolated build copy.
- Real Tomcat test for allowlisted proxy forwarding, plus default-mode spoofed-header rejection tests.
- Local packaged JAR health `UP`, browser registration/login back to `/`, anonymous access gate, 12 public consulting links, legacy POST rejection and 6 desktop/mobile widths (320–1440px).
- GitHub Actions have been pinned, but a new remote Actions run and production deployment have not been claimed as validated.
