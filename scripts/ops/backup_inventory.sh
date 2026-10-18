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
directory_mode="$(stat -c '%a' -- "$directory")"
[[ "$directory_mode" =~ ^[0-7]{3,4}$ ]] || exit 2
(( (8#$directory_mode & 077) == 0 )) || {
  echo 'CRITICAL: backup directory is readable by group/others' >&2; exit 1;
}
now="$(date +%s)"
newest=0
for archive in "${archives[@]}"; do
  [[ -f "$archive" && ! -L "$archive" && -f "$archive.sha256" && ! -L "$archive.sha256" ]] || {
    echo 'CRITICAL: unsafe archive or checksum file' >&2; exit 1;
  }
  for item in "$archive" "$archive.sha256"; do
    mode="$(stat -c '%a' -- "$item")"
    [[ "$mode" =~ ^[0-7]{3,4}$ ]] || exit 2
    (( (8#$mode & 077) == 0 )) || {
      echo 'CRITICAL: backup file is readable by group/others' >&2; exit 1;
    }
  done
  expected_line="$(cat -- "$archive.sha256")"
  actual="$(sha256sum "$archive" | cut -d ' ' -f1)"
  [[ "$expected_line" == "$actual  $(basename "$archive")" ]] || {
    echo 'CRITICAL: checksum mismatch or malformed filename/manifest' >&2; exit 1;
  }
  # Derive age from UTC backup filename, not mtime (copying an old archive resets mtime).
  filename="$(basename "$archive")"
  [[ "$filename" =~ ^growlab_([0-9]{4})([0-9]{2})([0-9]{2})T([0-9]{2})([0-9]{2})([0-9]{2})Z_[A-Za-z0-9]+[.]dump$ ]] || {
    echo 'CRITICAL: nonstandard backup filename; cannot verify capture time' >&2; exit 1;
  }
  timestamp="$(date -u -d "${BASH_REMATCH[1]}-${BASH_REMATCH[2]}-${BASH_REMATCH[3]} ${BASH_REMATCH[4]}:${BASH_REMATCH[5]}:${BASH_REMATCH[6]} UTC" +%s)" || {
    echo 'CRITICAL: invalid backup creation timestamp' >&2; exit 1;
  }
  [[ "$timestamp" -le "$now" ]] || {
    echo 'CRITICAL: backup creation timestamp lies in the future' >&2; exit 1;
  }
  (( timestamp <= newest )) || newest="$timestamp"
done
age="$(( (now-newest)/3600 ))"
if (( now-newest > hours*3600 )); then
  echo "CRITICAL: newest verified backup older than ${hours}h (age ~${age}h)" >&2
  exit 1
fi
echo "PASS: ${#archives[@]} checksum-verified archives; newest backup ~${age}h old."
