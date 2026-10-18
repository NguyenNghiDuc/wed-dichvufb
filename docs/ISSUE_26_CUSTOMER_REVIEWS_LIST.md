# Issue #26 — customer review history

`GET /campaigns/reviews?page=0` displays the logged-in customer's previously submitted service reviews, 20 per page, with links to their own campaigns.

`GET /api/v1/customer/reviews?page=0&size=20` returns a bounded page (size 1–50; page 0–10000) of private DTOs and `Cache-Control: no-store`. The query applies owner to SQL, sorts stably by created timestamp then ID, and never returns internal actors, email or username.

Only the original owner can access a reviewed campaign detail; another account's submissions are absent from both list and API. Empty account, bad paging, and anonymous access are covered by integration tests. No public review publishing, editing or deletion endpoint.
