# Customer discovery + confirmation, sprint 1 (18/10/2026)

**Owner scope:** customer UX and checkout only; no payment/provider automation, database/Flyway schema migration, workflow/CI YAML change.

## Delivered
- **107/108:** public `GET /dich-vu` server-side search with diacritic-insensitive Vietnamese query (max 100 chars), platform, minimum/maximum price, and allowlisted order by price ascending/descending/name. Invalid ranges and arbitrary sort values return HTTP 400. All items come from `ServiceCatalogService.available()`, not raw request data or a static fake price array.
- **109:** browse the 4 platforms x 3 service types (up to 12 options) with current price cards, compare and follow links to existing detail pages; never render disabled plans.
- **119:** recommendations of other *active* service plans on the same platform on each existing service detail page, including current server catalog prices. Public listing linked from the existing homepage and detail pages.
- **115:** opt-in post-submit owner-scoped confirmation `GET /campaigns/{id}/created` showing actual persisted order status, price, platform and date. Updated `campaign_form.html` sends hidden `receipt=true`, old POST callers with no `receipt` remain redirected to `/campaigns` to preserve existing clients and tests.
- **Security:** receipt URL is protected by existing Spring Security login and `CampaignOrderService.findMineById(username,id)` owner lookup. POST still requires CSRF. No request-provided price trusted, no guarantee of external fulfillment.
- Responsive scoped `gl-*` CSS for catalog and receipt only (maintains W1/W3/W9 styles).

## Tests
`mvn -Dtest=CustomerDiscoveryAndReceiptIntegrationTest test` plus `mvn clean verify`. Verify public no-login catalog, search without tone marks, valid/invalid price filters, disabled plans, server-side dynamic price, CSRF, ownership, anonymous redirects, SEO text escaping.

## Integration notes
Developed from main revision `06d2a248665e04e25d6d37f8efd60d1cc4a4e20b` after latest site nav changes. Keep this as Draft until exact HEAD Java CI PostgreSQL16/Flyway + JAR/Docker and CodeQL PASS, plus desktop/mobile browser QA.
Java 25 upgrade and CSS synchronization are separate W9/W10-reviewed PRs; **do not overwrite their workflow files or replace full style.css** while integrating.

## Next sprint
- 112: owner-scoped draft storage with explicit delete, expiry and no password/OTP fields; requires schema/migration coordination.
- 116: verified timeline presentation based on existing CampaignEvent history.
- 117/118: scoped secure file attachment and conversation linked to order, with antivirus/storage limits and role checks.
- Finish email/OAuth/API and real payment only with authorized environments and full security/UAT gates.
