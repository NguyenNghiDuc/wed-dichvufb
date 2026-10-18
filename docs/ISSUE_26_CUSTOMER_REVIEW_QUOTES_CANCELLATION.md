# GrowLab W2: review search, quote comparison and optional cancellation reason

Feature branch: `feature/issue26-customer-services-20261018`, draft PR #33.

## Owner-scoped review search and summary

`GET /campaigns/reviews?rating=2&q=keyword&page=0` (HTML) and
`GET /api/v1/customer/reviews?rating=2&q=keyword&page=0&size=20` (JSON).
Rating is 1..5 or omitted. Query is a literal, case-insensitive substring in
the review's feedback (max 80 characters), so % and _ have no LIKE wildcard
meaning. Both result and count queries enforce the signed-in principal's
`ownerUsername` in SQL. Page bound: 0..10000; API size: 1..50.
CSV and other customers' reviews are unaffected.

`GET /api/v1/customer/reviews/summary` returns
`total, average, oneStar, twoStars, threeStars, fourStars, fiveStars`.
An empty user has total 0 and average 0.0. Rating counts are aggregated in
the database with an owner predicate, never by loading all review texts.
The customer review HTML page shows total, average and five-star count.
This summary covers all the owner's ratings regardless of current search filters.
Private JSON responses include `Cache-Control: no-store` and no staff/owner metadata.

## Live quote comparison

`GET /api/v1/campaign-orders/quotes` returns a private, read-only list
of current **enabled** catalog service quotes:
`serviceType, displayName, priceVnd, currency, provisional`.
The campaign creation form also lists these server-driven prices, excluding
disabled packages. Client `priceVnd` query fields have no effect.
Quotes are provisional; at order creation the existing `catalog.quote`
is re-read on the server, so a customer can neither set nor approve a price.
This endpoint does not charge, hold funds, or auto-purchase anything.

## Customer cancellation reason (no new schema)

`POST /campaigns/{id}/cancel` (web) and
`POST /api/v1/campaign-orders/{id}/cancel` (API) accept optional
`reason` as a form/query parameter. After verifying ownership under row
lock, only a PENDING_REVIEW campaign may cancel.
The optional reason is trimmed, at most 200 characters, with ISO control
characters rejected. It is appended to an existing `campaign_events`
description for the **owner-scoped** history endpoint and escaped HTML UI/CSV.
Omitting the reason preserves prior behavior. No client-provided price,
status, or actor can be set by this feature. State changes require CSRF.
Foreign/nonexistent campaign IDs produce the same HTTP 404.

## Verification / release gates

H2 integration tests:
`CustomerReviewSearchIntegrationTest`,
`CustomerQuoteComparisonIntegrationTest`,
`CustomerCancellationReasonIntegrationTest`, plus the existing
`CustomerReviewListIntegrationTest`, `CampaignReviewsIntegrationTest`,
`CampaignOrderIntegrationTest`, and campaign history/CSV tests.

No migration was added in this slice. However PR #33 already contains
Flyway V10–V13 from earlier work and collides with W3/W6 versions.
**Do not merge** before W7 #41 allocates safe versions and runs
the combined PostgreSQL migration chain/validate. Require latest HEAD
Java CI, CodeQL, owner/CSRF/XSS review and staging browser testing.
