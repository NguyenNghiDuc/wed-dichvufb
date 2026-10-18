#!/usr/bin/env bash
# W10 local safety gate. Checks tracked files only; never prints credential values.
set -Eeuo pipefail
[[ $# == 0 ]] || { echo "Usage: $0" >&2; exit 2; }
command -v git >/dev/null || { echo 'Git required' >&2; exit 2; }
command -v grep >/dev/null || { echo 'grep required' >&2; exit 2; }
[[ "$(git rev-parse --is-inside-work-tree 2>/dev/null)" == true ]] || {
  echo 'Run within a Git repository' >&2; exit 2;
}
root="$(git rev-parse --show-toplevel)"
cd -- "$root"
bad=0
while IFS= read -r -d '' path; do
  base="${path##*/}"
  case "$base" in
    .env|.env.*.local|*.pem|*.p12|*.pfx|id_rsa|id_ed25519|*.keystore)
      echo "BLOCK: tracked credential-like file: $path" >&2
      bad=1 ;;
  esac
  if [[ -f "$path" && ! -L "$path" ]] &&
       grep -Iq . "$path" 2>/dev/null &&
       grep -Eiq -- '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----' "$path"; then
    echo "BLOCK: tracked private-key material in: $path" >&2
    bad=1
  fi
done < <(git ls-files -z)
[[ "$bad" == 0 ]] || {
  echo 'FAIL: tracked credential material; inspect privately and rotate exposed keys.' >&2
  exit 1
}
echo 'PASS: tracked-file credential signatures/forbidden filenames not found.'
echo 'NOTE: not a full secret scanner; inspect Git history and use approved security scanning.'
