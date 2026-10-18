# TikTok: server catalog, orders and account settings

## What is implemented

- `/`: public TikTok profile URL → country → server → quantity → calculated VND quote. Only enabled, configured servers appear. Empty catalogs disable ordering; no sample pricing or supplier claims are seeded.
- `/tiktok/orders/new`: authenticated quote review. POST `/tiktok/orders` revalidates URL, country, limits, enabled state and server revision. The server calculates prices; client-supplied prices are ignored. A changed server revision requires reviewing the new quote.
- `/tiktok/orders`: owner-scoped pagination and filters by order ID/status. `/tiktok/orders/{id}` includes the saved price/server snapshot, notes and paginated history. Only pending requests may be cancelled; a confirmation dialog is provided.
- `/admin/tiktok/servers`: ADMIN-only creation, editing and enabling/disabling servers. Country is ISO 3166-1 alpha-2; prices are integer VND **per follower**, not per 1,000. Unit prices: 1–1,000,000 VND; quantity bounds: 1–1,000,000. Maximum total: 10^12 VND. Disable rather than delete to preserve historical orders. Concurrent edits detect stale versions.
- `/admin/tiktok/orders`: ADMIN-only filtered and paginated request queue and details. Allowed internal transitions: PENDING_REVIEW → APPROVED → IN_PROGRESS → COMPLETED, or REJECTED while pending/approved. Customers may CANCEL only while pending. State changes are locked, recorded and produce owner notifications.
- `/profile`: edit display name, change password with current-password confirmation, revoke all sessions with current-password confirmation. Password changes use bcrypt and rotate the account authentication revision. Other sessions are rejected on their next request; current session is logged out. Settings password attempts are limited per account and application instance. Email changes, email recovery and OTP remain unavailable pending verified delivery.
- `/notifications`: owner-scoped pagination, unread filter, individual read and owner-scoped mark-all-read. State-changing endpoints require CSRF.

## Setup and operation

Java 17 and Maven >=3.8 remain required. H2 development uses Hibernate schema updates. PostgreSQL production uses additive Flyway V10 (account fields) and V11 (server/order/history tables), then Hibernate validation. Back up production before migrations; do not modify existing migration checksums. Authentication revision defaults to zero for existing accounts.

No default admin is created. Use the README's reviewed trusted-account promotion procedure, then sign back in. Go to `/admin/tiktok/servers`, enter accurate server details, prices and limits, and explicitly enable receiving requests. Until at least one server is enabled, the public form stays unavailable.

Orders are **manual requests**, not supplier submissions. There is no automatic fulfillment, balance debit, refund, real payment or guarantee of growth. The total is an estimate, not proof of payment. No supplier credentials are stored or requested by this module. Connect a real authorized provider in a separate integration before claiming automatic delivery. Never mark requests completed merely to produce public activity metrics.

The existing consulting campaign, support, content, staff and catalog workflows remain separate. Customer order/activity lists never show other customers' identities or links; the public counter is an aggregate of accepted requests.

## Validation

The test suite covers database-backed ordering, URL and amount validation, server states/revisions, historic price preservation, permissions, CSRF, status transitions, cancellation and pagination. Account tests check names, password validation/hashing and revocation. Notification tests check ownership and bulk-read behavior. PostgreSQL integration tests also validate migrations against the complete entity schema.
