# Issue #26 — customer dashboard service breakdown

Authenticated `GET /api/v1/campaign-orders/summary/services` returns counts grouped by service from **one owner-scoped database query**. It excludes all foreign accounts and returns an empty JSON list for accounts without orders. It has `Cache-Control: no-store`.

The dashboard shows the same count per service on desktop and mobile, independent of the current paging/filter state; labels explicitly clarify that rejected/cancelled orders are included. No order price, staff identity or payment data is exposed. MockMvc integration checks owner isolation and anonymous denial.
