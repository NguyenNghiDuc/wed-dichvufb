#!/usr/bin/env bash
# W10. Explicit opt-in for app DB. Fixture mode touches only ephemeral test containers.
set -Eeuo pipefail
umask 077
usage() {
  printf 'Usage: BACKUP_DIR=/secure/path %s --fixture | --db\n' "$0" >&2
  exit 2
}
[[ $# == 1 ]] || usage
case "$1" in
  --fixture)
    : "${RESTORE_TEST_PASSWORD:?RESTORE_TEST_PASSWORD required for isolated fixture}"
    service=fixture-db
    compose=(docker compose -f compose.yaml -f compose.recovery.yaml --profile recovery)
    ;;
  --db)
    [[ "${GROWLAB_BACKUP_CONFIRM:-}" == "YES" ]] || {
      echo 'Refusing app database backup without GROWLAB_BACKUP_CONFIRM=YES.' >&2
      exit 2
    }
    service=db
    compose=(docker compose -f compose.yaml)
    ;;
  *) usage ;;
esac
command -v docker >/dev/null || { echo 'Docker CLI unavailable' >&2; exit 2; }
[[ -n "${BACKUP_DIR:-}" && "$BACKUP_DIR" == /* ]] || {
  echo 'BACKUP_DIR must be an explicit absolute directory outside the repository' >&2
  exit 2
}
directory="$BACKUP_DIR"
[[ ! -L "$directory" && "$directory" != "/" ]] || {
  echo 'Refusing symbolic-link/root backup directory' >&2
  exit 2
}
mkdir -p -- "$directory"
directory="$(cd -- "$directory" && pwd -P)"
repo_root="$(pwd -P)"
case "$directory/" in
  "$repo_root/"*) echo 'Refusing to store database archive inside repository/Docker build context' >&2; exit 2 ;;
esac
[[ "$(stat -c '%u' -- "$directory")" == "$(id -u)" ]] || {
  echo 'Backup directory is not owned by current operator' >&2; exit 2
}
mode="$(stat -c '%a' -- "$directory")"
[[ "$mode" =~ ^[0-7]{3,4}$ ]] || { echo 'Invalid directory mode' >&2; exit 2; }
(( (8#$mode & 077) == 0 )) || {
  echo 'Backup directory must have no group/other permissions (choose a restricted location)' >&2
  exit 2
}
temporary="$(mktemp "$directory/.growlab-XXXXXXXX.part")"
cleanup() {
  [[ -z "$temporary" || ! -e "$temporary" ]] || rm -f -- "$temporary"
}
trap cleanup EXIT
trap 'echo "Backup failed; no complete archive published." >&2' ERR
if [[ "$service" == "db" ]]; then
  # Resolve database credentials only inside the container. Never echo passwords.
  "${compose[@]}" exec -T "$service" sh -eu -c \
    'exec pg_dump --format=custom --no-owner --no-acl -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$temporary"
else
  "${compose[@]}" exec -T "$service" \
    pg_dump --format=custom --no-owner --no-acl -U fixture -d growlab_fixture > "$temporary"
fi
[[ -s "$temporary" ]] || { echo 'Empty database archive' >&2; exit 1; }
"${compose[@]}" exec -T "$service" pg_restore --list < "$temporary" >/dev/null
token="$(basename "$temporary" .part | tr -cd 'A-Za-z0-9')"
name="growlab_$(date -u +%Y%m%dT%H%M%SZ)_${token}.dump"
published="$directory/$name"
[[ ! -e "$published" ]] || { echo 'Destination collision' >&2; exit 1; }
mv -- "$temporary" "$published"
temporary=
chmod 600 -- "$published"
(cd "$directory" && sha256sum "$name" > "$name.sha256")
chmod 600 -- "$published.sha256"
printf 'Backup complete: %s\n' "$published"
printf 'Checksum: %s.sha256\n' "$published"
