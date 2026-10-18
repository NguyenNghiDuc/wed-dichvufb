# W7 acceptance tracker — Issue #41

Do not mark a task complete without its exact commit, CI run and (where relevant) PostgreSQL 16/Flyway evidence. This is a release gate, not a progress claim.

| # | Acceptance criterion | Status |
|---|---|---|
| 1 | Resolve W2/W3/W6 migration collisions in combined tree | BLOCKED |
| 2 | Validate stacked W3 PR #44 V20–V25 SQL equivalence | PARTIAL: original SQL inspected; #44 patches are renames, exact content/hash not verified |
| 3 | Approve nonoverlapping versions after DB history check | BLOCKED |
| 4 | Verify checksums of applied migrations | BLOCKED: shared history unavailable |
| 5 | Review flyway_schema_history for each environment | BLOCKED: read-only DB access needed |
| 6 | Validate migration dependency graph | PENDING |
| 7 | Verify table/column/constraint/index creation order | PENDING |
| 8 | PostgreSQL 16 empty database migration test | PENDING |
| 9 | Upgrade test from main V9 with seeded data | PENDING |
| 10 | Flyway migrate and validate on isolated PostgreSQL 16 | PENDING |
| 11 | Run existing PR #39 collision guard on combined checkout | PENDING |
| 12 | Check foreign keys and referential integrity | PENDING |
| 13 | Check unique constraints and indexes | PENDING |
| 14 | EXPLAIN ANALYZE test-only query plans | PENDING |
| 15 | Review pagination columns and indexes | PENDING |
| 16 | Check migration concurrency and transaction isolation | PENDING |
| 17 | Compare data counts and checksums before/after upgrade | PENDING |
| 18 | Isolated backup/restore rollback rehearsal | PENDING |
| 19 | Document numbering policy across W1–W10 | PARTIAL: documented, unverified in integrated workflow |
| 20 | Test integrated W2/W3/W6 migration sequence | BLOCKED |
| 21 | Verify migration script content equivalence after renumbering | PENDING |
| 22 | Verify second Flyway migrate is a no-op | PENDING |
| 23 | Verify SQL checks and CI use the same combined commit SHA | PENDING |
| 24 | Capture team approvals, staging and security review | PENDING |
| 25 | Produce final 25/25 evidence ledger before release | PENDING |

## Version allocation (proposal only)

Current W2 PR #33 contains V10–V13; W6 PR #37 contains V10–V12; W3 PR #35 contains V10–V15; W3 PR #44 proposes V20–V25. Conditional allocation: W2 V10–V13, W6 V14–V16, W3 V20–V25. The original W6 V10→V14, V11→V15, V12→V16 mapping preserves the required W6 index dependency.

## 10-workstream policy

W1 authentication, W2 customer, W3 support, W4 admin, W5 AI, W6 payment, W7 database, W8 notifications, W9 CI, W10 DevOps: **no team owns a permanent numerical Flyway range**. Before adding a version, reserve the next unused version in Issue #41 after verifying latest main and all open PRs. W7 approves cross-team ordering; feature owners approve SQL changes. One merge at a time, then rebase and rerun PostgreSQL 16 migration tests. Already-applied migration scripts are immutable; use new forward-only migrations after approval. Never merge on green branch CI alone when integrated versions conflict.

## Evidence links

- Issue: https://github.com/NguyenNghiDuc/wed-dichvufb/issues/41
- W7 Draft PR: https://github.com/NguyenNghiDuc/wed-dichvufb/pull/50
- Existing guard: scripts/flyway_collision_check.py from merged PR #39
- Execution gates: docs/W7_FLYWAY_INTEGRATION_GATE.md

## Reporting contract

After every W7 code/test/commit turn, publish all 25 rows and status counts. VERIFIED means working code, relevant passing tests, successful exact-HEAD CI, and acceptance evidence; documentation or a draft PR alone never qualifies. Status legend: VERIFIED, CODED_UNVERIFIED, PARTIAL, NOT_STARTED, BLOCKED.

## W3 SQL dependency audit (18 October 2026)

Inspected the six additive SQL patches in PR #35: V10 index on support_messages(ticket_id,created_at,id); V11 adds support_tickets.priority and queue index; V12 adds chat_messages.read_at and unread index; V13 adds support_tickets.assignee_username and index; V14 creates support_assignment_events with FK to support_tickets(id) and ticket/timestamp index; V15 adds support owner/status/pagination index. No destructive SQL appears in these six PR #35 patches. Verify underlying table/column names in main V1–V9 and compare PR #44 resulting SQL blobs before acceptance; #44 is a rename patch, not independent evidence of unchanged checksums. FK and index creation still require PostgreSQL 16 execution.
