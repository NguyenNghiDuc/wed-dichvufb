# CodeQL SARIF helper exact-source self-test — 2026-11-05

Tracking: [Issue #13](https://github.com/NguyenNghiDuc/wed-dichvufb/issues/13), [roadmap Issue #3](https://github.com/NguyenNghiDuc/wed-dichvufb/issues/3), [Draft PR #38](https://github.com/NguyenNghiDuc/wed-dichvufb/pull/38).

## Exact-source verification

- Repository: `NguyenNghiDuc/wed-dichvufb`
- Branch: `feature/codeql-csrf-export-triage-20261018`
- Reviewed HEAD before this documentation commit: `8a6159c6fdafc00c2cb4fde430eb71240005764f`
- Source path: `scripts/codeql_sarif_integrity.py`
- Git blob SHA fetched from GitHub: `4c06abd2d07166d3a469d16ee9b437d163f28df5`
- Reconstructed file bytes were checked with the Git blob SHA-1 algorithm and matched that exact blob SHA before execution.
- Runtime: Python `3.13.5`
- `python scripts/codeql_sarif_integrity.py --self-test`: **26/26 PASS**, exit code 0.
- `python -m py_compile scripts/codeql_sarif_integrity.py`: **PASS**.

This corrects the earlier status: the exact repository source has now been reconstructed byte-for-byte (matching the Git blob SHA) and its embedded tests have been executed. The earlier manually reconstructed copy with a non-matching blob SHA is superseded by this exact-source verification.

## What this verifies — and what it does not

The helper's embedded tests cover malformed/missing SARIF, absent or unsuccessful invocation evidence, invalid notifications, path-graph exporter errors including warning/note notifications, expected commit provenance, bounded input, UTF-8 handling, and a valid log/SARIF pair.

This is a local unit/self-test of the helper only. It is **not** a CodeQL vulnerability scan, does not authenticate an uploaded artifact, does not prove complete CSRF path coverage, and does not replace review of published Code Scanning analyses/alerts.

## Current repository evidence checked 2026-11-05

- Main SHA: `084f85dcaf1b39553298da18e021b7cbd688c990`.
- Draft PR #38 HEAD before this documentation commit: `8a6159c6fdafc00c2cb4fde430eb71240005764f`.
- Exact-HEAD Java CI PR [41272930580](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41272930580) and push [41272927747](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41272927747): **SUCCESS**; Java 17.0.21, 175 tests, 0 failures/errors/skips, PostgreSQL 16.15/Flyway, JAR startup smoke and Docker build/validation pass.
- Exact-HEAD CodeQL PR [41272930540](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41272930540) and push [41272927763](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41272927763): workflow **SUCCESS**, but decoded logs still contain three `java/csrf-unprotected-request-type` SARIF path-graph errors for `edit`, `registerPage`, and `loginPage`.
- Main scheduled CodeQL [41434423770](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41434423770), on main SHA above, also logs the same three graph errors despite workflow success.
- CodeQL CLI/query pack in those runs: CLI `2.27.3`, Java queries `1.11.13`; Action `4.39.0`.
- No downloadable SARIF Actions artifact was returned for these CodeQL runs. The connected GitHub reader rejects Code Scanning analyses/alerts endpoints; published SARIF and alerts therefore remain unreviewed.

## Remaining release gates — NOT MET

- [ ] Execute the isolated Java 17/Spring minimal reproduction with the exact CodeQL CLI/query pack and retain logs plus SARIF.
- [ ] Inspect the actual published SARIF and Code Scanning alerts for partial/missing path coverage.
- [ ] Complete independent browser-session CSRF tests on isolated staging (login/register/session/logout and role-protected writes).
- [ ] Review signed-webhook replay/idempotency and concurrency behavior.
- [ ] Obtain independent security reviewer approval.
- [ ] W9 review any integration of the helper into CI; this commit does not modify workflow files.

No query suppression, handler renaming, W9 workflow edits, production deployment, real transaction, artificial engagement, repository secrets, force push, or direct main write. Keep PR #38 **Draft** and Issue #13 open until all applicable gates are evidenced and reviewed.
