# W4 — Admin catalog price history CSV (Issue #28)

Status: draft, requires owner UAT and security review.

`GET /admin/catalog/history/export.csv?type=<enum>&actor=<exact>&page=<0..10000>`
returns only the selected **20-row page** from the existing catalog audit query, sorted by recorded time and ID descending, with filters checked before database pagination.

Columns: event timestamp, actor, service code, former/new VND quote, and former/new enabled flags. Never includes credentials, external payment references, customer emails, balances or untrusted HTML. Text is CSV-quoted and formula-escaped using the existing audited helper. No-store, nosniff and attachment headers are set.

Only ADMIN may download, with tests for USER, STAFF and anonymous rejection, invalid filters and paging. The audit database is not immutable and does not certify money received or refunded. Verify browser/mobile UX, Excel CSV safety, real PostgreSQL performance, backup and CodeQL findings before acceptance. No production deployment.
