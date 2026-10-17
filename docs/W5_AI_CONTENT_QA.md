# W5 — AI content, draft search and official API guardrails (#29)

Status: **implementation prototype — not accepted for production**. Builds on PR #32, already merged.

## Draft search
- `GET /content-calendar?platform=TIKTOK&status=DRAFT&q=title&page=0`
- Owner predicate is applied in the database along with platform/status/title/body filters; never takes owner from client query.
- Searches are case-insensitive; SQL LIKE wildcards `%`, `_` and backslash are literal.
- 100-character query maximum, no control characters, 20 rows per page, page 0..10000. HTML is escaped by Thymeleaf. Filters persist in next/previous links.
- Editing, unscheduling and deleting drafts remain the implementations merged in PR #32. No auto-publish.
- Acceptance: H2 and PostgreSQL tests, mobile UI, realistic query plans, owner isolation and staging review.

## AI requests (requires owner-supplied key)
`POST /api/v1/ai/generate` (the existing `/api/v1/ai/caption` stays compatible). Session auth and CSRF required.

Example body:
```json
{
  "platform": "TIKTOK",
  "topic": "Study techniques",
  "audience": "University students",
  "task": "CONTENT_PLAN",
  "language": "EN",
  "days": 7
}
```

Tasks: `CAPTION`, `HASHTAGS`, `TIKTOK_IDEAS`, `YOUTUBE_SHORTS`, `HEADLINES`, `CONTENT_PLAN`, `OPTIMIZE`.
Languages: `VI` (default) and `EN`; tasks default to `CAPTION`. `CONTENT_PLAN` supports 7–30 days; `OPTIMIZE` requires `existingContent` (up to 3000 characters). The AI response is a **suggestion** and must be edited and fact-checked before use, never an authenticated analytics claim.

Without `GROWTH_AI_API_KEY`, the API returns 503; no synthetic AI-generated output is returned. Only the configured HTTPS provider endpoint is called. Secrets are read from environment; never commit credentials. **User topics, audience, and existing draft text are transmitted to that provider when configured**; do not enable externally without user notice/consent, data handling policy and a privacy review.

Limits (development, **per server instance only**): 5 requests per user/minute and 25 requests per user/24h, via in-memory limiter; limits apply even if the provider is unavailable. Provider completion token budget = 450 normally or 1800 for plans. No retries on quota/network/5xx to avoid accidental duplicate cost. 429 -> 503, timeout -> 504, upstream/unparseable -> 502, missing key -> 503. No provider response body/key is surfaced to users. This is not a durable billing ledger or a globally enforced financial cap. Production requires shared atomic quotas, provider usage/cost accounting, provider budget alarms and reconciliation.

## Official social APIs — not connected
- Facebook Pages API / Instagram Graph API: require approved Meta app, minimal granted scopes, account/page ownership checks, per-token rate limits and access review. Do not request passwords, scrape private content or enable publishing without user authorization.
- TikTok Developer API: OAuth grants and review are required for eligible live integrations; Direct Post uses `video.publish` authorization and unaudited clients face restricted visibility. Official reference: https://developers.tiktok.com/docs/en/content-posting-api-get-started and https://developers.tiktok.com/docs/en/content-sharing-guidelines
- YouTube Data API: existing public channel summary is read-only. Verify quota and error codes with official docs: https://developers.google.com/youtube/data/reference/rest/v3/errors and https://developers.google.com/youtube/data/determine_quota_cost
- Instagram professional account APIs are described in Meta's official collection: https://www.postman.com/meta/instagram/documentation/6yqw8pt/instagram-api
- No platform token storage/OAuth callback/cross-account insights/autopost currently delivered. Pending approved apps, credentials, user consent, platform review and staged end-to-end tests.
- Analytics, best posting time or performance claims must derive from authorized verified metrics; no invented data.

## Quality gates
- Unit tests for task defaults, formats, quota/error mapping, timeouts, invalid provider JSON, credential isolation.
- MockMvc tests for authentication, CSRF, request validation, rate limiting and missing-key fail-closed.
- Content search tests for cross-owner isolation, literal wildcard semantics, filter/page preservation, validation and HTML escaping.
- CI Java/PostgreSQL/Flyway/JAR/Docker, CodeQL and staging/manual browser tests are required; CI success is not security sign-off. Do not check roadmap items as accepted until review.

## Performance preview from explicitly user-supplied numbers (no provider integration)
- `POST /api/v1/content-analytics/preview`: authenticated and CSRF-protected JSON containing 1–100 `posts`, each with `platform`, `publishedAtUtc`, `views`, `likes`, `comments`, `shares` and `saves`.
- Validates nonnegative bounded integer counts (views 1..1e9), previous two years of UTC timestamps, positive view counts, payload array size, 30 requests/user/minute per instance. The preview is **stateless**; numbers are not retained in a new DB table, sent to LLM or submitted to a social provider.
- Response source always `USER_SUPPLIED_UNVERIFIED`; computes interaction count per 100 views, **not validated engagement metrics**, and provides a conspicuous warning. A candidate posting hour UTC is **only shown** with >=12 same-platform posts and >=4 posts for at least 3 distinct hours, grouped by recorded UTC post hour. This is a descriptive historical signal, not an optimal-time guarantee. Otherwise the hint is null.
- The white–orange calendar provides a 1-post interactive preview and openly states that one post cannot yield a posting-time suggestion. Advanced multi-post input remains API-only; there is no fabricated imported history or third-party API access.
- Unit tests cover arithmetic, insufficient sample size, mixed platforms, timestamp/count limits; MockMvc tests cover authentication, CSRF, request validation and provenance; test on PostgreSQL for draft search literal wildcard/tenant scoping.
- Provider-verified performance analytics and account-specific posting recommendations are **NOT** implemented: require approved OAuth scopes, authenticated owner identities, official API data, quota limits, and consent. Do not label roadmap #046 or #047 accepted from this preview.
