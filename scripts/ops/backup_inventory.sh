#!/usr/bin/env bash
# Read-only backup freshness/integrity check. Never deletes or decrypts backups.
set -Eeuo pipefail
[[ $# == 3 && "$1" == "--check" ]] || {
  echo "Usage: $0 --check BACKUP_DIRECTORY MAX_AGE_HOURS" >&2
  exit 2
}
directory="$2"
hours="$3"
[[ -d "$directory" && ! -L "$directory" &&
   "$hours" =~ ^[0-9]{1,3}$ && "$hours" -ge 1 && "$hours" -le 720 ]] || {
  echo 'Invalid backup directory or age threshold (1..720 hours)' >&2; exit 2;
}
command -v sha256sum >/dev/null || exit 2
command -v stat >/dev/null || exit 2
shopt -s nullglob
archives=("$directory"/growlab_*.dump)
[[ "${#archives[@]}" -gt 0 ]] || { echo 'CRITICAL: no GrowLab archives' >&2; exit 1; }
now="$(date +%s)"
newest=0
for archive in "${archives[@]}"; do
  [[ -f "$archive" && ! -L "$archive" && -f "$archive.sha256" && ! -L "$archive.sha256" ]] || {
    echo 'CRITICAL: unsafe archive or checksum file' >&2; exit 1;
  }
  expected="$(cut -d ' ' -f1 < "$archive.sha256")"
  [[ "$expected" =~ ^[0-9a-fA-F]{64}$ ]] || {
    echo 'CRITICAL: invalid checksum manifest' >&2; exit 1;
  }
  actual="$(sha256sum "$archive" | cut -d ' ' -f1)"
  [[ "$actual" == "$expected" ]] || {
    echo 'CRITICAL: checksum mismatch' >&2; exit 1;
  }
  timestamp="$(stat -c '%Y' -- "$archive")"
  [[ "$timestamp" -le "$now" ]] || {
    echo 'CRITICAL: backup timestamp lies in the future' >&2; exit 1;
  }
  (( timestamp <= newest )) || newest="$timestamp"
done
age="$(( (now-newest)/3600 ))"
if (( now-newest > hours*3600 )); then
  echo "CRITICAL: newest verified backup older than ${hours}h (age ~${age}h)" >&2
  exit 1
fi
echo "PASS: ${#archives[@]} checksum-verified archives; newest backup ~${age}h old."
