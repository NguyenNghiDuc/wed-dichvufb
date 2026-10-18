# Issue #26 — legacy owner-only order summary

Legacy customer page `/order/list` shows total historical orders and summed quoted amounts, independent of current page and filter. This is clearly labeled **dự kiến**, not paid revenue, settled balance or delivery.

Authenticated read-only endpoint `GET /api/v1/customer/legacy-orders/summary` returns `orderCount` and `amountVnd` with `Cache-Control: no-store`. Only `Principal` supplies owner; no request parameter can change it.

One SQL aggregate returns both count and sum using a 64-bit numeric result, avoiding N+1 queries. Integration tests cover multi-orders, cross-owner isolation, empty user, forged owner query and dashboard render. No migration and no Admin/chat/payment edits.
