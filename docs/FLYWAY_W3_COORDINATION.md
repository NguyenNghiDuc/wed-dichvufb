# GrowLab #27 — Flyway migration coordination

This is an isolated draft preparation branch, not an applied database migration.

W3 support originally reserved V10–V15 in PR #35, which conflicts with customer
PR #33 (V10–V12) and payment PR #37 (V10–V12). This proposal only renames W3's
**unmerged** migration source files and keeps their SQL blob contents unchanged:

| Original draft | Proposed version | Purpose |
| --- | --- | --- |
| V10 | V20 | Support message timeline |
| V11 | V21 | Ticket priority |
| V12 | V22 | Chat read receipts |
| V13 | V23 | Staff claim |
| V14 | V24 | Assignment audit |
| V15 | V25 | Owner ticket queue index |

Integration order matters: merge PR #33 with V10–V12 first, coordinate
a distinct range for PR #37 (e.g., V13–V15), then apply W3 V20–V25.
Do **not** merge W3 first and later introduce lower versions without a
verified Flyway out-of-order plan.

Safety gates:
1. Confirm no original draft versions were applied in staging/production
   by inspecting `flyway_schema_history` and checksums. If any have been
   applied, **do not rename/delete historical applied migrations**;
   design forward-only repairs with all relevant owners.
2. Obtain explicit coordination with PR #33 and #37 owners.
3. Bring the merged `main` guard scripts from PR #39 into the eventual
   integration checkout; run `python3 scripts/flyway_collision_check.py`.
4. Run full Java, Flyway/PostgreSQL, CodeQL and browser UAT after integration.
5. Keep this stacked PR and PR #35 Draft until the above gates are met.

No shared database was changed by this proposal. It is not permission to deploy.
