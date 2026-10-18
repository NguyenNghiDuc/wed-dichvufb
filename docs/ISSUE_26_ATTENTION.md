# Issue #26 — customer order attention (read-only)

- Customers get a visual support prompt for declined campaigns (REJECTED), pending review 48h+, or in-progress 168h+. These are **provisional internal attention thresholds**, not contractual delivery SLAs.
- The UI includes a support ticket link; it does not automatically refund, approve, reassign, or alter the order.
- `GET /api/v1/campaign-orders/{id}/attention` is owned-only, no-store, read-only, and returns no staff identity.
- Pure unit tests cover time threshold boundaries and closed-order suppression. Integration tests cover ownership / anonymous / HTML.
- Owner and human staging review required; consider later synchronizing thresholds with Admin SLA configuration after coordination.
