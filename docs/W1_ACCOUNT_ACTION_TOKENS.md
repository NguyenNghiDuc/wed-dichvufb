# W1 account action challenge primitive — NOT ACTIVE (#25)

**Status:** Testable offline groundwork only. This is not an email verification or password reset feature and is NOT ready for a public endpoint.

- `AccountActionTokenStore` is an isolated Java class (not a Spring bean) providing 256-bit SecureRandom opaque bearer tokens for `VERIFY_EMAIL` and `RESET_PASSWORD`, with 15-minute expiry, atomic one-time consumption, exact account/purpose binding, and reissue invalidation.
- Only SHA-256 digests are retained in memory; the issuing object redacts bearer data from `toString()`. The raw token must eventually be delivered **only** through a configured and verified outbound email provider, not exposed in HTTP response or log.
- No database migrations, outgoing emails, authentication endpoints, account updates, API keys, or production deployment have been added.
- The current store is intentionally **in-memory and single-node**. On application restart tokens disappear; across replicas redemption is inconsistent. It is NOT suitable for a production password recovery flow.
- Before activation: owner supplies authorized SMTP or email API, shared persistent hashed-token storage, secret-safe transactional consume+password change, delivery-failure semantics, anti-enumeration response, per-account and IP resend limits, contact/email ownership verification, revocation, privacy review and staging UAT.
- JUnit tests cover expiry at the exact boundary, purpose/account separation, reissue, token redaction and concurrent one-time redemption.

Do not mark roadmap email verification (#2) or password reset (#3) complete until all external and security gates are met.
