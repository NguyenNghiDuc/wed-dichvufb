# CodeQL CSRF SARIF triage — 2026-10-18

Tracking: #13; roadmap gate: #3.

## Known evidence

CodeQL Java `java/csrf-unprotected-request-type` emitted `got start node that does not appear in graph` for `AuthController.loginPage` and `registerPage` in multiple runs. A successful workflow conclusion does not establish complete query coverage.

## Required investigation

1. Inspect published Code Scanning alerts and download the SARIF artifact for the affected run. Check whether results for the affected paths are absent or partial.
2. Record CodeQL CLI and action versions, Java 17 version, query pack, run ID and exact exporter logs. Reproduce on a minimal controller with GET login/register handlers.
3. On an isolated staging environment, verify session cookie attributes and CSRF token issuance, login/register POST with missing/invalid token (must reject), valid token (must follow expected authentication/validation flow), and the narrowly exempted signed webhook with invalid signature, timestamp and replay attempts (must reject).
4. Distinguish application vulnerability from analyzer/export defect; fix confirmed vulnerabilities, otherwise report upstream with a minimal reproduction. Do not suppress the query to turn CI green.
5. Attach security review, PostgreSQL/Flyway integration results, Java tests, production JAR and Docker checks, and owner UAT evidence before any production approval.

## Release gate

This document is a checklist, not evidence that any security test passed. Keep issue #13 open and security approval blocked until findings are classified and independently reviewed. Do not use real credentials, payments or production deployments for reproduction.

## Revalidation — 2026-11-12 (source-level query evidence; acceptance still blocked)

### Upstream query source inspected

Inspected `java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll` at CodeQL commit `07502a696c4bcc496f12e0ef0c0f3ef3b56523c6` (blob `80d100d3d9e11b538720bfb177639148d73074b6`) and compared it with upstream `main`; the fetched file contents/blob are identical.

Relevant query structure:

- `SpringCsrfUnprotectedMethod` includes Spring `@GetMapping` handlers.
- `NameBasedStateChangeMethod` classifies method names using the query's interesting/uninteresting-word regular expressions.
- `unprotectedNameBasedStateChange(sourceMethod, sinkMethod)` requires `sinkMethod = sourceMethod`, i.e. the same `CallPathNode` may satisfy both ends of this branch.
- The path-graph edge predicate `relevantEdge(pred, succ)` is based on `CallGraph::edges(pred, succ)` plus reverse-flow predicates.

This is direct source evidence for the **self-source/sink (potential zero-edge path) hypothesis**. It does not, by itself, prove that the SARIF exporter is defective or that every such result must fail graph serialization. A clean-room run and inspection of the actual SARIF are still required.

### Current repository evidence

- Main remains `084f85dcaf1b39553298da18e021b7cbd688c990`.
- Main CodeQL scheduled run [42609944869](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/42609944869), job `129135485800`, analyzed that exact main SHA with CodeQL CLI 2.27.4 / Java query pack 1.12.0. The log reports 197/197 Java files scanned, then three `java/csrf-unprotected-request-type` graph errors for `edit`, `registerPage`, and `loginPage`, followed by SARIF export/upload completion.
- Main Java CI [39548204218](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/39548204218), job `119051109194`, ran on 2026-10-21 with Java 17.0.20.1: 173 tests, 0 failures/errors/skips; PostgreSQL 16.15, Flyway, JAR health smoke, and Docker image build passed.
- Draft PR #61 remains one commit ahead / zero behind main at `85b8a5c43bd0a6592a6003bad4da562195a15125`. Its CodeQL PR run [42561672962](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/42561672962), job `128979240262`, reports six graph errors: the three application handlers plus test-only `reproEdit`, `reproRegister`, and `reproLogin`.
- PR #61 Java CI push [42561632543](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/42561632543) and PR [42561672879](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/42561672879): Java 17.0.21, 173 tests with zero failures/errors/skips, PostgreSQL 16.15/Flyway, JAR health smoke and Docker image build passed. The workflow does not start the built Docker image for a separate runtime smoke.

### Roadmap identity

Issue #3 links to the 105-item document on `feature/roadmap105-security-sprint` (branch commit `e545d82c37544df72af3c4c0589193b71f4da497`, file blob `2668d9b60ec2b931487fa284effa847689899470`). The same path on main is a distinct 125-item roadmap (blob `fa378f988ea6dddf2cdc5bf019f17db35511f092`). Do not conflate or silently replace these versions.

### Remaining acceptance gates — NOT MET

- Execute the planned standalone clean-room Java 17/Spring reproduction with the exact CodeQL CLI/query pack and retain logs plus SARIF.
- Inspect the actual published SARIF and Code Scanning alerts for incomplete/missing path results.
- Complete independent browser-session CSRF tests on isolated staging.
- Review signed-webhook HMAC, replay, idempotency and concurrency.
- Obtain independent security review.
- Add Docker container-runtime smoke coverage.
- W9 must review any CodeQL helper/workflow integration; this PR does not change workflow files.

Keep PR #38 and PR #61 **Draft**. Do not suppress the CSRF query, rename handlers to evade the heuristic, weaken CSRF, edit W9 workflows, merge to main, deploy production, perform real transactions, use secrets, or force-push.

