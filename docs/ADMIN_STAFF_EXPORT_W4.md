# W4 — filtered STAFF/ADMIN page export (Issue #28)

Status: **draft implementation, not owner accepted**.

`GET /admin/staff/export.csv?role=STAFF|ADMIN|ALL&username=<exact>&enabled=true|false&page=0`
returns exactly the filtered **current page (20 rows maximum)** of the Admin staff directory. The CSV includes only username, role, enabled flag and current active assignment count. No email, password, IP address or personal contact details.

The endpoint uses the existing parameterized, database-paged `AdminStaffDirectoryService` and the pre-existing CSV formula escaping helper. Invalid roles/pages/names are rejected with HTTP 400; anonymous, USER and STAFF sessions cannot export. Content-Disposition attachment, no-store and nosniff headers protect the response from inadvertent browser caching/content sniffing.

This report is not employee payroll data, a full-database export, a compliance audit, or an authenticated proof of work performed. Owner needs to verify CSV opening with Excel/LibreOffice and staging authorization, choose retention/export policy, check real production query plans and sign off. Existing CodeQL CSRF SARIF issue #13 and manual security review remain unresolved. No production deployment or financial transactions.
