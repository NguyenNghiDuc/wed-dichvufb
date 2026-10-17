# YouTube Data API read-only adapter – resilience acceptance

Status: PROTOTYPE, NOT OWNER-ACCEPTED. Related to roadmap #058/#096 and issue #3.

## Scope
- Validates official channel identifier before any outbound call.
- Uses only an HTTPS GET against `www.googleapis.com/youtube/v3/channels`, to retrieve public metadata when the owner configures `growth.youtube.api-key`.
- The application continues to fail closed with HTTP 503 when no official API key is present.
- Request timeout maps to HTTP 504; quota/rate limiting (429) maps to HTTP 503; other upstream failures and malformed or oversized responses map to HTTP 502; missing public channel maps to HTTP 404.
- Provider response bodies and secret-bearing URI values are never returned in client errors.
- No automatic retries or follower/video interaction; quota safety prioritized over pretending requests succeeded.
- A package-private HTTP transport allows completely local, deterministic tests with mock responses and no provider credentials or network calls.

## Verification needed
- Unit tests for success, invalid channel, missing key, HTTP 403/429/500, 504 timeouts and malformed/empty responses.
- Existing MockMvc integration tests for unauthorized access and unavailable configuration.
- Java CI with real PostgreSQL/Flyway/JAR/Docker and CodeQL runs; JaCoCo and security findings review.
- Real provider integration requires OWNER-supplied credentials for authorized staging ONLY. No credentials are collected or stored in this PR.
- Official API quota, key restrictions, rate-limits, terms and observability must be reviewed before launch.
- Existing unrelated CodeQL `java/csrf-unprotected-request-type` SARIF graph processing errors still require review; a passing job does not clear them.
