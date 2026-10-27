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
