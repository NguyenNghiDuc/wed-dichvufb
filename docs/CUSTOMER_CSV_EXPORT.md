# Customer Campaign CSV Export

Endpoint: `GET /campaigns/export.csv` (requires a signed-in session).
Optional filters `orderId` (positive numeric ID) and `status` (CampaignStatus enum)
match the filters on `/campaigns`; the download link preserves both.

- Only rows belonging to `Principal.getName()` are queried; there is no owner parameter.
- Hard maximum: 5000 rows, in pages of 50 (application memory is bounded by the maximum).
- Columns: ID, platform, service, status, quoted price (not paid amount),
  created timestamp, target URL, brief.
- UTF-8 BOM for Excel compatibility; CSV cells are quoted and spreadsheet
  formula-like prefixes are escaped by the shared admin CSV helper.
- Invalid filters return HTTP 400; unauthenticated users must sign in.
- Header `Cache-Control: no-store` prevents browser/proxy caching.

Automated test: `mvn -Dtest=CustomerCampaignCsvIntegrationTest test`.
The full repository CI should also pass. Production authorization, large-volume
performance, and CSV spreadsheet display still require staging/UAT checks.
