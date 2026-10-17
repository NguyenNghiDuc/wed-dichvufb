# Issue #26 — customer service quote read endpoint

Authenticated `GET /api/v1/campaign-orders/quote?serviceType=CONTENT_PLAN` returns current server-catalog price, display name, currency VND, and `provisional=true`, with `Cache-Control: no-store`.

There is **no price, owner, status or approval update endpoint** here. Client-supplied price parameters are ignored. Disabled services and unknown service codes yield 400; session is mandatory. During order creation, the price is retrieved anew from the catalog, so an earlier preview never authorizes a discounted price.

Integration tests check current/updated prices, forged price fields, disabled service, invalid/missing type and anonymous request. Run the full Java/Postgres suite and staging tests before accepting.
