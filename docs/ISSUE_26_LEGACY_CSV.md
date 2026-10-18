# Issue #26 — owner-only legacy order CSV

`GET /order/export.csv` downloads up to 5,000 legacy customer orders in 50-row batches, sorted by creation time and ID, filtered **inside the database** by the authenticated principal. It never accepts an owner request parameter or a client-supplied price.

The export includes a UTF-8 BOM, fixed filename, `Cache-Control: no-store`, quoted RFC4180-style CSV cells, and spreadsheet formula escaping for attacker-controlled notes/service/link fields. The UI at `/order/list` links to the download.

Automated tests cover account isolation, price derived from server, CSV escaping, linked HTML UI, and anonymous protection. Production browser/Excel testing and PostgreSQL performance analysis remain required.
