# Campaign status history API (customer scope)

`GET /api/v1/campaign-orders/{id}/history?page=0&size=20`

- Requires the existing authenticated session. GET does not mutate an order.
- Returns a chronological Spring Page of status events for a campaign owned by the signed-in customer.
- Valid ranges: `page=0..10000`, `size=1..50`. Invalid values return HTTP 400.
- Unknown or foreign campaign IDs both return HTTP 404; no cross-account event/count leak.
- Each entry contains `id`, `previousStatus`, `status`, `description`, `createdAt`. Internal `actor` usernames are deliberately excluded.
- Uses `Cache-Control: no-store`; supports existing Spring Security protections.
- No database migration: indexed `campaign_events` and current repository are reused.

Verification: `mvn -Dtest=CampaignHistoryIntegrationTest test` and `mvn test`.
CI, staging UAT and security review are required before merging. This does not mark roadmap item #037 as accepted.
