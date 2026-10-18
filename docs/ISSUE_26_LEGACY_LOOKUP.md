# Legacy order lookup (Issue #26)

Customer may search legacy orders by numeric ID at `GET /order/list?orderId=123`. The filter is applied alongside the authenticated owner **inside** the JPA query, not in Java after data retrieval. Unknown or another customer's ID results in an empty list, avoiding data leaks.

Pagination and the CSV download maintain the same owner-scoped ID filter. `GET /order/export.csv?orderId=123` uses the same validated server query. Negative/zero ID and out-of-range pages return 400. No price or status mutation is added. Mobile CSS belongs only to the customer page.

Regression checks: owner data, foreign ID, CSV consistency, empty result, invalid ID and other authenticated account.
