# W1 password recovery cryptographic groundwork (#25)

**NOT ENABLED**: no public recovery endpoint, no token in UI/logs, no SMTP delivery, no OAuth/2FA configured. Do not claim the user can already reset a password.

`PasswordRecoveryTokenCodec` implements a provider-independent, time-bounded HMAC-SHA256 signed URL-safe recovery token (15 minutes) with an independent 128-bit nonce, account binding, and password-revision binding. After successful password rotation, old tokens fail signature verification, including across instances that have the same external HMAC key. The codec has no default generated secret and is intentionally not registered as an injectable Spring bean.

**Critical next integration requirements before enabling reset:**
1. Owner must connect a real authorized email delivery service and prove the address is verified. Never return token material through API responses, logs or analytics.
2. Configure a stable secret of at least 32 unpredictable bytes through the deployment secret manager; do not commit it to GitHub.
3. Implement reset consumption in a database transaction with a locked user row; verify token under that lock against the *current* encoded hash, apply a separately tested strong-password policy, rotate hash and revoke all prior sessions via credential revision. The codec alone does NOT mark tokens consumed; fail before success leaves the token valid until expiry.
4. Require per-IP/account request limits, generic account-unknown responses, CSRF protection where session cookies are used, no-store, strict link origin controls and link referrer protection.
5. Test multi-node concurrency/clock skew, token reuse after successful rotation, unknown/unverified addresses, password byte limits, SMTP retry/outage and rollback; independent human security review required.

No Flyway changes were made: W7 owns schema upgrades. Email verification, token delivery and live password reset remain unresolved blockers pending owner configuration/approval.
