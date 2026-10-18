# Issue #26 — CSV export for a single campaign history

Customer GET `/campaigns/{id}/history.csv` downloads chronological status history.
Owner identity is derived from the authenticated session; nonexistent and other customers' campaigns both return 404. The export is read-only and includes a UTF-8 BOM and spreadsheet-formula escaping.

Columns: `created_at,previous_status,new_status,description`. Employee/admin actor usernames are deliberately excluded. Up to 1000 rows per export (page size 50), no unlimited event reads; filename is fixed, `Cache-Control: no-store`.

Automated checks: authenticated owner gets history, cross-customer attempt returns 404, anonymous user redirects, no internal actor in file, formula escaping. As with other CSV export, production browser/Excel checks and staging data review remain pending.
