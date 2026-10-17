# Customer campaign dashboard — acceptance checklist

## Scope
- Web: `GET /campaigns` shows four white/orange summary cards: total, pending review, active (approved + in progress), and completed.
- JSON: `GET /api/v1/campaign-orders/summary` returns numeric `total`, `pendingReview`, `approved`, `inProgress`, `completed`, `rejected`, and `cancelled`.
- Counts are computed from a single owner-scoped, grouped database query. The owner is read **only** from the authenticated principal, never from query parameters.
- The summary covers all the owner's campaigns and is unaffected by list pagination or filters; empty accounts receive zeroes.
- Values are *order counts*, not paid revenue or externally verified delivery.

## Regression checks
1. Create campaigns under two separate user accounts, progress selected orders through admin-reviewed status transitions, and verify the numbers.
2. Test that another authenticated account never sees those counts, including a forged `?owner=...` parameter.
3. Check anonymous requests redirect to login, and the `/campaigns` HTML loads correctly with filters active.
4. Check phone and desktop layouts and keyboard navigation in staging.

## Release status
This patch adds automated H2 integration tests but requires CI and human acceptance before production. No changes to table schema, payment processing, pricing or OAuth permissions.
