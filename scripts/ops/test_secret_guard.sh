#!/usr/bin/env bash
# W10 simulated Git index tests. No real secrets, network or production changes.
set -Eeuo pipefail
umask 077
[[ -f scripts/ops/secret_guard.sh ]] || { echo 'Run from repo root' >&2; exit 2; }
command -v git >/dev/null || { echo 'git missing' >&2; exit 2; }
guard="$(pwd -P)/scripts/ops/secret_guard.sh"
temp="$(mktemp -d)"
trap 'rm -rf -- "$temp"' EXIT
git init -q "$temp/repo"
printf 'Synthetic W10 test repository\n' > "$temp/repo/README.md"
git -C "$temp/repo" add README.md
(cd "$temp/repo" && bash "$guard") > "$temp/output"
grep -q '^PASS:' "$temp/output"
printf 'SYNTHETIC_ONLY=NOT_A_REAL_SECRET\n' > "$temp/repo/.env"
git -C "$temp/repo" add .env
if (cd "$temp/repo" && bash "$guard") > "$temp/output" 2>"$temp/err"; then
  echo 'FAIL: tracked .env passed secret guard' >&2; exit 1
fi
! grep -q 'NOT_A_REAL_SECRET' "$temp/output" "$temp/err"
git -C "$temp/repo" rm -q --cached .env
rm -- "$temp/repo/.env"
# Construct signature dynamically so the TEST SOURCE does not itself trip the scanner.
marker='PRIVATE'
printf '%s\n' "-----BEGIN $marker KEY-----" 'SYNTHETIC INVALID MARKER ONLY' \
    "-----END $marker KEY-----" > "$temp/repo/note.txt"
git -C "$temp/repo" add note.txt
if (cd "$temp/repo" && bash "$guard") > "$temp/output" 2>"$temp/err"; then
  echo 'FAIL: tracked private-key marker passed secret guard' >&2; exit 1
fi
echo 'PASS: local secret guard rejects staged .env and private-key markers without printing values.'
