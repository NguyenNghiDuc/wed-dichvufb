# W1 — Current session transparency (Issue #25)

- `GET /account/sessions` now shows only the active browser's session creation time, last-access UTC, idle timeout and bounded request address/User-Agent summary, as server-observed metadata.
- Rendered with Thymeleaf `th:text`, which escapes HTML. Strings are stripped of control characters and limited to 80/160 Unicode code points, without persisting contact data or opening an external fingerprint API.
- `X-Forwarded-For` and User-Agent are untrusted user/proxy-originated hints, not proof of physical location or device identity. When deployed behind a proxy, the displayed address can be the proxy's address.
- Does not claim to list past IPs or all active devices; that requires explicit retention/privacy acceptance and schema ownership by W7 before real production implementation.
- Existing authenticated password-proof POST `/account/sessions/revoke` retains CSRF and credential-revision revocation. Unit/MockMvc regression checks no anonymous read and XSS-safe presentation.
