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

Limits (development, **per server instance only, only when provider is configured**): 5 requests per user/minute and 25 requests per user/24h, via in-memory limiter; limits apply even if the provider is unavailable. Provider completion token budget = 450 normally or 1800 for plans. No retries on quota/network/5xx to avoid accidental duplicate cost. 429 -> 503, timeout -> 504, upstream/unparseable -> 502, missing key -> 503. No provider response body/key is surfaced to users. This is not a durable billing ledger or a globally enforced financial cap. Production requires shared atomic quotas, provider usage/cost accounting, provider budget alarms and reconciliation.

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

## Manual suggestion workflow and safe fallback
- The assistant displays provider text as text only; the **explicit** button “Đưa gợi ý vào form bản nháp” fills the new draft editor **without submitting it**, without overwriting existing title/body, and without publishing. The operator must review, edit and click Save; any output longer than 3000 characters must be shortened before copying.
- UI enforces platform/task compatibility (TikTok ideas only TikTok, Shorts only YouTube), requires original draft for OPTIMIZE, and disables unnecessary fields so unrelated text is not sent to the AI provider.
- When `/api/v1/ai/generate` returns 503 for a `CAPTION` request with topic <=100 characters, the UI may fetch the pre-existing `GET /api/v1/content-suggestions` local deterministic templates from the **same origin**. Clearly labels the output **TEMPLATE_ONLY / không phải nội dung do AI tạo**. It never pretends the fallback output is LLM text, and never calls paid or social APIs for the fallback. This workflow remains user-triggered; no scheduled publishing.

## Deterministic plan fallback (7–30 days)
- `GET /api/v1/content-templates/plan?platform=TIKTOK&topic=education&days=7` requires an authenticated user and applies an in-memory rate limit of 20 calls/minute/user. The server rejects missing platform, invalid control characters, topics outside 3..100 characters, and day counts outside 7..30.
- Response includes `mode: TEMPLATE_ONLY`, an exactly matching count of numbered content **suggestions**, and an explicit warning: not AI, not derived from platform data, not saved/scheduled/published.
- If the AI endpoint responds 503 for CONTENT_PLAN, the calendar may show this **non-AI** fallback. Result is plain textarea text, and moving it into the saved draft form remains a separate explicit action. It does not impersonate provider output or reclassify a local plan as AI.
- Unit/API tests cover 7, 14, 30 days, input bounds, authentication and result labelling. Production acceptance still requires manual UI/mobile review and privacy/consent confirmation.

## AI capabilities (readiness, not a billing statement)
- Authenticated `GET /api/v1/ai/capabilities` reports configured/unconfigured generative AI, supported formats/languages, 5 requests/minute and 25 requests/24h per user, plus `quotaScope: PER_INSTANCE_ONLY`.
- This is an explicit warning that limits are per single application instance, not a cross-server quota or monetary budget. The endpoint exposes neither provider credential nor private settings and never calls the AI provider.
- Missing-key attempts return 503 without reducing the billable request budget, while a separate 30/minute/user per-instance anti-spam limit remains in force. The capabilities API exposes this distinction as `unconfiguredRequestsPerMinute: 30`.
- `publishingEnabled: false` is returned, including when an AI key is configured. This endpoint is informational and does not authorize API posting.

- Both current local deterministic fallbacks produce Vietnamese templates. The UI only offers them when the requested language is `VI`, explicitly warns if `EN` was selected and the paid AI provider is unavailable, and never represents Vietnamese fallback content as English generation.

## UTC schedule range filtering
- Calendar GET also accepts optional `fromUtc` and `toUtc` in ISO local date-time notation **representing UTC**, e.g. `2026-10-18T10:00`. These are only query filters, not instructions to publish.
- With both bounds present, the start must not exceed the end and the query window may not exceed one year. Predicates for owner, platform, status, keyword, and `scheduledAtUtc` are combined in the same SQL query; no cross-account or unbounded-fetch filtering.
- The white–orange search form preserves time bounds during pagination; H2 and PostgreSQL tests cover UTC filters, invalid date bounds and owner isolation.

## W5 incremental hardening — batch UI and integer precision
- AI official HTTP response streaming reads at most 200,001 bytes, rejects responses beyond 200,000 bytes and closes its response stream, rather than first materializing an arbitrary response as a Java string. Unit tests exercise the byte bound and truncated reads. HTTP timeout and provider quotas still require live staging verification.
- The white–orange content calendar now offers an **optional bulk preview**: paste 1–100 newline-separated observations as `PLATFORM,YYYY-MM-DDTHH:mm,views,likes,comments,shares,saves` (UTC). This is a deliberately restricted comma-delimited numeric format, not a generic CSV file upload. Input is validated client side and again in the backend; no provider calls or persistence. Result uses `textContent`; any hour hint remains explicitly tentative and labelled `USER_SUPPLIED_UNVERIFIED`.
- Metric counters use strict integer JSON deserialization; fractional values, numeric strings, overflows and out-of-range values must not be silently rounded or truncated into plausible analytics. Integration tests cover invalid input and the 12-record minimum needed for an illustrative UTC hour hint. Users must never interpret the preview as third-party verified engagement.
- For an **approved live integration**, provider request quota/capacity must be separately tracked and reconciled with the official dashboards. YouTube Data API quotas and uploads have changed in 2026: check https://developers.google.com/youtube/v3/determine_quota_cost and https://developers.google.com/youtube/data/reference/rest/v3/videos/insert rather than hardcoding historical 1,600-unit upload costs.
- TikTok read-only Display API needs OAuth grants such as `user.info.basic` and `video.list`, not anonymous scraping: https://developers.tiktok.com/docs/en/display-api-get-started. Direct posting additionally requires reviewed `video.publish` and explicit user consent: https://developers.tiktok.com/docs/en/content-posting-api-get-started. The current GrowLab implementation does not post or obtain platform access tokens.
- Meta Instagram Login for professional accounts has distinct reviewed permissions such as `instagram_business_basic` and `instagram_business_content_publish`: https://www.postman.com/meta/workspace/instagram/documentation/23987686-9386f468-7714-490f-9bfc-9442db5c8f00. Do not assume these permissions apply to personal profiles or permit posting by default.

## Bilingual offline templates for more content tasks
- Authenticated, quota-bounded `GET /api/v1/content-templates/ideas?platform=TIKTOK&topic=study&task=TIKTOK_IDEAS&language=EN` supports `HASHTAGS`, `HEADLINES`, `TIKTOK_IDEAS`, `YOUTUBE_SHORTS` in `VI` and `EN`. It validates task/platform compatibility, topic length and control characters, and allows up to 20 calls/minute/user **per instance**.
- Every response says `mode: TEMPLATE_ONLY`; these are **deterministic writing outlines**, not LLM output, real performance trends, verified hashtags, licensed data, recommendations based on analytics or platform publishing actions. Unsupported OPTIMIZE requests reject with 400 rather than pretending to rewrite text.
- If the actual `POST /api/v1/ai/generate` endpoint responds 503, the calendar can present the corresponding local idea/headline/hashtag template through a separate same-origin GET. It validates task/platform/language and uses a text-only textarea; copying into drafts still requires an explicit button followed by manual Save. No user credentials or OAuth secrets are involved.
- Manual review must reject misleading health/finance/legal or other claims; generated templates do not imply factual verification.

## Content workspace styling (PR #34)
- `src/main/resources/static/content-workspace.css` is scoped under the calendar's `.content-workspace` container and does not modify the shared global stylesheet used by the other five workstreams.
- White/orange cards and forms use existing CSS theme variables, responsive two-column/one-column layouts, horizontal table scrolling on small screens, readable status blocks, visible keyboard focus and reduced-motion support.
- This is a code-level implementation only; manual Safari/iOS/Android layout and keyboard accessibility testing in staging is still required.

## Official social API requirements endpoint
- Authenticated `GET /api/v1/integrations/requirements` returns FOUR reference records (Facebook Pages, Instagram professional accounts, TikTok, YouTube): example minimal read permissions, distinct publishing permissions, official documentation links, and warnings. **All entries explicitly have accountConnected=false, publishingEnabled=false, ownerConsentRequired=true.**
- This endpoint is an access/consent checklist, **not a connection status**, login flow, token minting endpoint, or authorisation to call a provider. Existing `GET /api/v1/integrations` continues to expose read-only YouTube key readiness separately.
- Scope names and official rules change; verify chosen endpoints and granted app review permissions using current official documentation before enabling OAuth, analytics import, or publishing. No token storage, scheduled auto-posting, follower manipulation or credential collection was added.
- MockMvc tests enforce authentication and never expose tokens/secrets or claim accounts connected.
