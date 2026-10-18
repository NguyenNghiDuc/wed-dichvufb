# W10 — PostgreSQL backup / isolated restore runbook (#47)

**Scope:** operational scripts for a reviewed staging environment. **No database has been backed up or restored by these commits.** The offline Bash guard test needs only Bash; the full restore test requires an authorized local Docker engine. This is NOT an approval to use production data.

## Failure and safety boundary

- Application backup runs **only** when the operator explicitly passes \`--db\` AND \`GROWLAB_BACKUP_CONFIRM=YES\`. It uses \`docker compose exec -T db\` and the database credentials already configured inside that container, not CLI password arguments.
- Restore script accepts **only** \`--sandbox\`, which hardcodes Docker Compose service \`recovery-db\`, user \`recovery\` and database \`growlab_recovery\`. It has **no** target host/DB override. Never use it for production.
- \`compose.recovery.yaml\` creates fixture and recovery Postgres 16 instances in **ephemeral tmpfs**, internal Docker network, no published ports, no durable volume, and no dependency on app/db. The full test loads exactly three synthetic rows, verifies the custom dump, rejects a tampered copy, restores to the separate empty database, checks row count and sum, rejects a second restore, and removes only fixture/recovery services on exit.
- \`pg_dump -Fc --no-owner --no-acl\` creates a portable archive; its filename is unique, directory mode 0700, archive/checksum mode 0600. Creation uses a temporary partial file which is not published on dump failure. \`pg_restore --list\` checks readability before rename.
- The \`.sha256\` sidecar is for **accidental corruption detection**, not cryptographic authenticity: someone able to rewrite both files can forge it. Restrict who can read/write backups; use authenticated encrypted off-site storage with managed keys for real personal data.
- \`pg_restore\` runs with \`--single-transaction --exit-on-error --no-owner --no-acl\` and refuses nonempty target schemas. Never feed an untrusted archive: PostgreSQL dump/restore programs should process backups only from trusted sources. Review dump content before restoring from a different trust domain.

## Docker-free guardrails (safe to run now)

From repository root:

\`\`\`bash
bash scripts/ops/test_guardrails.sh
\`\`\`

Covers Bash syntax, refusal before calling Docker if approval/test secret missing, nonexistent archive, bad checksum, and symbolic links. It uses a fake Docker executable and does not connect to any database.

## Fully isolated Docker test (requires **local test** Docker permission)

\`\`\`bash
bash scripts/ops/test_backup_restore.sh
\`\`\`

The test exports newly generated **synthetic** \`POSTGRES_PASSWORD\` and \`RESTORE_TEST_PASSWORD\` for its child Compose processes (no real credential required). It starts only fixture and recovery services, uses ephemeral DBs, and removes them on exit. Do not run concurrently with another W10 recovery test under the same Compose project; run in a separate clone/project if necessary. Verify Docker Engine and Compose v2 support \`up --wait\`, tmpfs, and \`exec -T\`. A workstation without Docker cannot certify this test.

Expected success marker: \`PASS: synthetic fixture backup, checksum rejection, isolated restore, data verification, overwrite protection.\`

### Manual synthetic commands

If you need to inspect the sandbox during review (not real data), generate a test-only random secret and run:

\`\`\`bash
export RESTORE_TEST_PASSWORD="$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')"
export POSTGRES_PASSWORD="$RESTORE_TEST_PASSWORD"
docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery \
  up -d --wait fixture-db recovery-db
# Never populate fixture with real customers, credentials or payment history.
# Use the end-to-end test above to generate, archive, restore and validate dummy data.
\`\`\`

Clean up sandbox services only:

\`\`\`bash
docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery \
  stop fixture-db recovery-db
docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery \
  rm -sf fixture-db recovery-db
unset RESTORE_TEST_PASSWORD POSTGRES_PASSWORD
\`\`\`

## Backup of an approved application database (not executed by this PR)

1. Confirm **which** Docker project/database is running, approved maintenance window, authorized operator, expected storage, encryption, retention and recovery objective. Record Git SHA, schema migration version, DB image version and backup timestamp; avoid printing secrets.
2. Confirm sufficient free disk space and protected directory, with encrypted offsite copy prepared.
3. In the approved environment only, explicitly run:

\`\`\`bash
export BACKUP_DIR="/path/to/approved/restricted/backup-directory"
export GROWLAB_BACKUP_CONFIRM=YES
bash scripts/ops/backup.sh --db
unset GROWLAB_BACKUP_CONFIRM
\`\`\`

4. Backups may contain personal, authentication, and transaction data: never commit, attach to a PR, or print their contents. Restrict filesystem permissions; do not copy actual data into fixture tests.
5. Validate backup existence, checksum, restore compatibility on a **separately approved isolated environment**, tables/row counts/constraints and service smoke without exposing identities. A successful \`pg_dump\` **alone** does not establish recoverability.
6. Keep **at least one** prior verified recovery point across deployments. Establish retention policy and offsite encryption with the owner before any automatic deletion; W10 scripts intentionally do not silently prune backups.


## Read-only freshness alert for approved backup directories

```bash
bash scripts/ops/backup_inventory.sh --check "/path/to/approved/restricted/backup-directory" 24
```

- Checks every `growlab_*.dump` and its `.sha256` sidecar without reading/exposing actual SQL rows or deleting archives. Fails if any checksum is invalid, missing, symlinked, has a nonstandard UTC filename or the most recent archive's **embedded creation timestamp** is older than the chosen 1–720-hour threshold (filesystem mtime is not trusted). Its exit status is designed for a separately approved scheduler/monitor; W10 does not create an unattended cron job or external alert connector.
- The Docker-free `test_guardrails.sh` checks a synthetic valid archive, then corrupts it and expects the inventory to fail. A passing checksum is **not** equivalent to successful recovery or secured encrypted off-site storage.
- Run checks with an operator who has read access to the protected backup directory, never as an unauthenticated web endpoint.

## Real incident recovery (manual approval only; no production restore code here)

- Stop public writes and identify failure; notify the incident lead and account for in-flight payment/signed webhook events with W6.
- Obtain an approved point-in-time snapshot or encrypted verified dump. **Never run** \`restore_sandbox.sh\` against an application database.
- Provision a **new isolated** PostgreSQL recovery instance with no access to app clients, verify provider/credentials/owner and SQL/migration compatibility, then follow DBA-approved \`pg_restore\` procedure with single-transaction and audited reconciliation.
- Preserve old volumes unchanged until recovery acceptance; run Flyway validation, user/order ownership tests, security/CSRF checks, and read-only smoke before any DNS switch.
- Reconcile payments, notifications and external side effects separately: restoring a DB does not undo delivered messages or settlements.
- Securely document RPO (recovery point), RTO (recovery time), recovery operator, verification timestamp and approval. Do not claim production RPO/RTO until rehearsed on real-equivalent staging hardware.
