# W4: bounded Admin activity timeline (Issue #28)

Status: Draft / requires security review, staging UAT and owner acceptance.

- Read-only `GET /admin/activity?area=ALL|ACCOUNTS|CATALOG|ASSIGNMENTS|ORDER_STATUS`, authorized only for `ROLE_ADMIN` through the existing security policy.
- Pulls at most 50 rows from each of four existing event tables, then displays at most the latest 50 globally, deterministically sorted.
- Distinguishes account role/lock audits, service price changes, staff assignment changes and **only Admin-initiated** campaign status updates.
- Reads only fields relevant to operations. No passwords, payment amounts, API keys, personal email addresses or free-form customer notes.
- This convenience list is NOT tamper-proof: source tables have no immutable offsite log, events can be retained/deleted according to database policy. All sources must be reviewed for audit completeness.
- No new schema or data migration, no mutation or production deployment. Logged-in STAFF/USER/anonymous cannot read the page.
- Regression tests cover role boundaries, malformed category, four-source aggregation and 50-row cap.
- Manual UAT required on mobile and real database volumes. Source page may omit events older than 50 records per stream; do not treat as complete history.
