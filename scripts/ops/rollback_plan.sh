#!/usr/bin/env bash
# W10: printable staging rollback checklist ONLY. Does not modify git, Docker or DB.
set -Eeuo pipefail
[[ $# == 3 && "$1" == "--plan" ]] || {
  echo "Usage: $0 --plan CURRENT_SHA PREVIOUS_KNOWN_GOOD_SHA" >&2
  exit 2
}
current="$2"
previous="$3"
[[ "$current" =~ ^[0-9a-fA-F]{7,40}$ && "$previous" =~ ^[0-9a-fA-F]{7,40}$ &&
   "$current" != "$previous" ]] || {
  echo 'Both revisions must be distinct Git commit SHAs (7–40 hex chars)' >&2
  exit 2
}
cat <<EOF
GrowLab W10 STAGING ROLLBACK PLAN — NO COMMANDS EXECUTED
Failed candidate: $current
Previously accepted candidate: $previous

1. Incident lead approves maintenance; halt incoming changes and inspect health/status.
2. Preserve current PostgreSQL volume, current container images and logs.
3. Locate the last VERIFIED backup and its validated SHA-256 plus restore proof.
4. Review Flyway schema changes. Never point old app binaries at incompatible schema.
   If database migration isn't backward compatible: STOP and involve DBA/W7.
5. In an isolated staging worktree, verify the recorded previous Git SHA, build it,
   run Java, PostgreSQL/Flyway and Docker smoke, compare image digest.
6. With written owner approval, replace only the staging app container from the
   previously tested immutable artifact. Preserve database and proxy state.
7. Re-run /actuator/health, /login, session/CSRF checks and critical owner flows.
8. Record timestamps, RPO/RTO, SHA/image digest, rollback approver and outcome.

WARNING: no database restore is implemented for live systems.
A code rollback cannot undo migrations, external callbacks or side effects.
EOF
