# Issue #26 — legacy order page bounded paging

Legacy `GET /order/list?page=N` previously materialized every owned record. The controller now requests 20 rows per page with an owner predicate applied in SQL, deterministic `createdAt DESC, id DESC` ordering, and visible prev/next controls. Invalid page input gives HTTP 400.

Existing `OrderService.list` stays for backwards compatibility but is no longer used by the web endpoint. Price remains calculated inside the server at the fixed legacy rate; customer-supplied price is ignored. This legacy form stores internal records only and does not provision social activity or process payments.

H2 integration checks cover 23 owned records over 2 pages, cross-owner isolation, zero records, invalid page and the fixed price. Stage performance on realistic PostgreSQL data remains pending.
