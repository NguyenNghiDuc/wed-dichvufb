# W7 Flyway integration release gate — Issue #41

Status: BLOCKED. This is an audit and version-reservation proposal, **not evidence that migrations have run on PostgreSQL 16**.

## Current competing versions (PR file lists, 18 October 2026)

- W2 PR #33: V10 customer campaign filter; V11 reviews; V12 contact profiles; **V13 legacy order filter indexes**. V13 is present in current PR files and supersedes the older V10–V12-only plan.
- W3 PR #35: V10–V15 support migrations. Stacked PR #44 proposes renaming these to V20–V25, preserving relative order. Verify SQL content and checksum against original files before accepting.
- W6 PR #37: V10 payment review indexes; V11 stable deposit history index; V12 payment amount constraints. V11 depends on W6 V10's index.

## Proposed reservations (conditional, not approved)

| Owner | Target versions | Merge order |
|---|---|---|
| W2 #33 | V10–V13 | First |
| W6 #37 | V14–V16 (original V10→V14, V11→V15, V12→V16) | Second |
| W3 #44 stacked onto #35 | V20–V25 | Third |

Do not merge W3 V20–V25 ahead of lower pending versions unless Flyway ordering is proven safe. Leave V17–V19 unallocated, not implicitly reserved. **Never rename migrations already applied to any shared database.** Obtain environment owners' signed history snapshots before any renumbering.

## Mandatory evidence

1. On each shared database, capture read-only history:
   `SELECT installed_rank, version, description, type, script, checksum, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;`
   Record environment and database identifier securely; do not publish secrets.
2. If any candidate migration version is applied, STOP. Compare checksum and SQL to the deployed artifact. Draft forward-only corrective migrations and obtain DBA approval; do not use `repair`, `baseline`, `outOfOrder` or destructive rollback.
3. Compare SQL for table/column existence, FK targets, unique constraints, index dependencies and index drop/create order. Confirm W6 V14 precedes V15.
4. Integrate the approved branches in a disposable checkout. Reuse `python3 scripts/flyway_collision_check.py` and `python3 -m unittest discover -s scripts -p 'test_flyway_collision_check.py' -v`; do not duplicate the guard.
5. In isolated PostgreSQL **16**, test fresh schema migrations, then upgrade from main V9 with representative seeded data; execute Flyway `migrate`, `validate`, and a second `migrate` to verify idempotent no-op. Check `flyway_schema_history` success and checksums against committed SQL.
6. Compare row counts and representative FK relationships before/after upgrade; inspect indexes and constraints via PostgreSQL catalogs. Use `EXPLAIN (ANALYZE, BUFFERS)` on representative **test-only** queries before recommending new indexes.
7. Rehearse `pg_dump` and `pg_restore` using disposable test databases; compare counts and integrity after restore. Never test destructive rollback against shared data.
8. Run Java 17 CI, PostgreSQL 16 integration tests, CodeQL, security review, staging UAT and owner sign-off on **the same combined commit SHA** before merge.

## Cross-team coordination

W2: confirm V13 and dependency graph. W6: approve conditional V14–V16 mapping and update references in its own PR. W3: validate #44 SQL equivalence and integration into #35. W7 owns audit/test evidence, not other teams' business logic.

Release remains blocked until the above evidence is attached to Issue #41.