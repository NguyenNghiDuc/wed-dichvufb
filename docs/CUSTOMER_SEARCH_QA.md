# Customer campaign lookup / filters — QA

Routes:
- Web: `GET /campaigns?q=...&platform=YOUTUBE&status=PENDING_REVIEW&page=0`
- API: `GET /api/v1/campaign-orders/page?q=...&platform=TIKTOK&status=APPROVED&size=20&page=0`

Rules:
- The authenticated session supplies the owner. No owner, user ID or sort column is accepted from clients.
- Search covers exact numeric campaign ID plus case-insensitive literal substrings of brief and target URL. The wildcard characters `%`, `_` and backslashes are escaped, not interpreted.
- Status and platform accept only declared enum values. Unknown values return HTTP 400.
- Search query max 100 characters, page 0–10000, page size 1–50; stable ordering by createdAt and ID descending.
- UI search filters survive next/previous pagination links.
- Flyway V9 indexes owner/status and owner/platform for production PostgreSQL.
- Historical list endpoint still returns first 50 only for backwards compatibility.

Verification:
- CI regression coverage for owner isolation, literal wildcard search, status, platform, exact ID, invalid inputs and anonymous access.
- Manual UAT should additionally inspect responsive layout, visual highlighting, localization, handling of very large datasets and performance under load.
- Passing CI cannot rule out all security vulnerabilities. No deployment/merge without review.
