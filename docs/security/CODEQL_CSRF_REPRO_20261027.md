# CodeQL CSRF SARIF reproduction plan — 2026-10-27

Tracking: [#13](https://github.com/NguyenNghiDuc/wed-dichvufb/issues/13), [#3](https://github.com/NguyenNghiDuc/wed-dichvufb/issues/3). This is an investigation plan, **not** a completed minimal reproduction or security sign-off.

## Reproduced in the existing repository

- Draft PR #38 at `cd3155e79f599cc87364cb16eea4636bede4f33e`; Java CI run `40268157139` / job `121394966349` passed (Java 17.0.20.1, 174 tests, PostgreSQL/Flyway, JAR smoke, Docker).
- CodeQL PR run `40268157428` / job `121394967182` concluded `success`. CodeQL CLI `2.27.3`, Java queries pack `1.11.13`.
- At 2026-10-26 04:17:34 UTC, the log printed three `java/csrf-unprotected-request-type` SARIF graph errors: `edit`, `registerPage`, `loginPage`. It then printed `Exported results to SARIF` and `Successfully uploaded results`.
- No downloadable Actions SARIF artifact was exposed for this run. **Published Code Scanning analyses/alerts and uploaded SARIF are not verified**; a successful upload is not proof of complete path coverage.

## Isolated minimal-reproduction experiment (NOT YET EXECUTED)

1. Use a throwaway local Java 17 / Spring Boot project, not a production or shared environment. Record the exact Spring Security version and CodeQL CLI/query pack versions. Never copy real credentials or customer data.
2. Begin with only a controller containing `@GetMapping("/login")` and `@GetMapping("/register")` that returns view names; add a `@PostMapping("/register")` handler separately. Keep CSRF enabled with the Spring Security defaults. Analyze with the same `java-security-extended` query suite.
3. Compare three variants separately: GET handlers only; GET plus POST handler; same handlers with the narrow signed-POST webhook CSRF exemption. Save CodeQL logs, SARIF, CLI/query pack versions and the exact source commit for each variant.
4. Run the same isolated variants with an updated CodeQL bundle and compare exporter errors. Do **not** disable `java/csrf-unprotected-request-type` to obtain a green result.
5. If the graph error reproduces in the minimal project, prepare a sanitized upstream CodeQL bug report. If not, bisect app-specific handlers and security configuration without removing CSRF protection.
6. A security reviewer must inspect actual Code Scanning alerts and SARIF for missing/partial results; workflow conclusion alone is insufficient.

## Independent application security gate (NOT YET COMPLETED)

On an isolated staging deployment with **test-only** accounts, verify token issuance and cookie attributes, valid/invalid/missing CSRF on login and registration, session rotation, logout, and role-protected writes. Verify the sole POST webhook exemption: unsigned/malformed HMAC and oversized payloads reject; signed events require idempotency/replay review. Existing MockMvc checks and the new unsigned/oversized webhook regression tests are **not** a replacement for independent browser tests.

Do not merge Draft #38 or approve production until published SARIF/alerts, independent security tests and reviewer approval are recorded. W9 owns workflow changes; this plan makes none.


## 2026-11-01 — upstream query-source analysis (hypothesis, not security sign-off)

### Exact run evidence

- Draft PR #38 HEAD before this documentation update: `fc286b7eb77988b0294da868e5f6e949561438e7`.
- CodeQL PR run [41005017259](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/41005017259), job `123897841307`: CodeQL CLI `2.27.3`, CodeQL Action `4.39.0`, Java query pack `1.11.13`.
- At 2026-10-30 04:50:56.873–04:50:56.879 UTC, SARIF export logged path-graph errors for `registerPage`, `loginPage`, and `edit`. Export then completed at 04:50:56.992 UTC, upload succeeded at 04:50:58.050 UTC, and analysis processing completed at 04:51:03.248 UTC. This confirms a partial path-graph export failure despite workflow success.

### Query-source explanation

Reviewed the upstream CodeQL query source at [CodeQL commit 950734fc80d48083479843e2284039832eaf54c7](https://github.com/github/codeql/tree/950734fc80d48083479843e2284039832eaf54c7):

- [CsrfUnprotectedRequestType.ql](https://github.com/github/codeql/blob/950734fc80d48083479843e2284039832eaf54c7/java/ql/src/Security/CWE/CWE-352/CsrfUnprotectedRequestType.ql) emits a path-problem using `source` and `sink`.
- [CsrfUnprotectedRequestTypeQuery.qll](https://github.com/github/codeql/blob/950734fc80d48083479843e2284039832eaf54c7/java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll) treats Spring `@GetMapping` handlers as unprotected request methods. Its name heuristic includes `login`, `register`, and `edit`; `loginPage`, `registerPage`, and `edit` therefore match the interesting-name pattern.
- The `unprotectedNameBasedStateChange` branch sets `sink = source` and excludes cases that also update a database. This creates a zero-length path candidate. The query's `edges` relation describes call-graph edges and does not add a reflexive self-edge. The three exporter messages (“start node that does not appear in graph”) are consistent with this zero-edge path shape.

### Application-side cross-check

- `AuthController.loginPage()` and `registerPage()` are `@GetMapping` handlers that only return the `login` and `register` view names.
- `ContentDraftController.edit()` is a `@GetMapping` handler that loads the current user's draft through `ContentDraftService.findMineById()`; that service method is annotated `@Transactional(readOnly=true)` and uses an owner-scoped repository lookup.
- The POST `revise` and `delete` handlers remain unsafe-method endpoints and continue to rely on Spring Security CSRF protection. The sole configured CSRF exemption remains the internal POST webhook, which still requires HMAC validation.

### Classification and remaining proof

This is a strong, source-backed explanation for a **query name-heuristic / zero-length path-graph edge case**, not evidence that these three GET handlers perform state changes and not proof that the whole application is CSRF-safe. Do not rename handlers or suppress the query to silence the error.

Still required before classifying the issue as an analyzer defect:
1. Execute the planned isolated Java 17/Spring minimal reproduction with the exact CodeQL CLI/query pack and retain its log and SARIF.
2. Inspect the actual uploaded SARIF and published Code Scanning alerts/analysis for missing or partial results.
3. Complete independent browser-session CSRF testing and signed-webhook replay/idempotency review on isolated staging.
4. Obtain security reviewer approval. W9 still owns workflow integration; this update changes no workflow.

No production deployment, real payment, real secret, or production data was used. Keep Issue #13 open and PR #38 Draft.
