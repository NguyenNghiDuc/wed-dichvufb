# CodeQL CSRF SARIF investigation — Issue #13

**Status:** investigation only; not a security approval.  
**Checked:** 2026-11-20  
**Main:** `084f85dcaf1b39553298da18e021b7cbd688c990`  
**PR #62:** `279591718c495d88a9a747d832c0a381aa3a6725` (Open, Draft; 7 ahead / 0 behind)

## 1. Latest exact-head CI evidence

The diagnostic self-edge control was added only to the isolated reproduction project. No production application source, Spring CSRF configuration, database migration, payment behavior, or runtime configuration changed.

| Workflow | Run / job | Result |
|---|---|---|
| Java CI | [44672986233](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986233) / `136078077959` | PASS |
| CodeQL combined app + fixture | [44672986192](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986192) / `136078078200` | Analyze SUCCESS; seven path-graph export errors |
| CodeQL fixture-only | [44672986192](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986192) / `136078077917` | Expected FAIL from the fail-closed SARIF gate |

Java CI exact-head details:
- Temurin Java 17.0.21+9.
- 173 application tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL 16.16 service and Flyway version guard passed.
- Maven `BUILD SUCCESS`.
- Production-profile JAR health smoke passed.
- Docker image build passed.
- Docker application-container runtime health smoke is not part of this workflow.

The isolated fixture Maven build also passed: 3 tests, 0 failures/errors/skips. Its CodeQL job uses `upload: never`; no fixture alerts are published.

## 2. Main baseline and workflow configuration

Main remains `084f85dcaf1b39553298da18e021b7cbd688c990`.

| Main workflow | Run / job | Result |
|---|---|---|
| Java CI | [39548204218](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/39548204218) / `119051109194` | PASS |
| CodeQL scheduled analysis | [43856565667](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/43856565667) / `133262231496` | Workflow SUCCESS, but three CSRF path-graph errors |

The main CodeQL log records CodeQL CLI 2.27.4 and `codeql/java-queries/1.12.0`, then reports missing graph nodes for `edit`, `registerPage`, and `loginPage` before saying results were uploaded. Main still pins CodeQL Action v4.39.0; PR #62 tests both init/analyze at the same immutable v4.39.1 commit.

Main branch metadata reports `protected=false`; the repository rulesets collection is empty. Branch-protection enforcement is therefore still a release blocker.

## 3. Query-source inspection and limits

The upstream CodeQL source snapshot previously inspected is commit
[`fe4820ab48ecd83a2ff590977f58beb27bc8ee10`](https://github.com/github/codeql/commit/fe4820ab48ecd83a2ff590977f58beb27bc8ee10):

- [CsrfUnprotectedRequestTypeQuery.qll](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll)
- [CsrfUnprotectedRequestType.ql](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/src/Security/CWE/CWE-352/CsrfUnprotectedRequestType.ql)
- [Upstream query test](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/test/query-tests/security/CWE-352/CsrfUnprotectedRequestTypeTest.java)

**Version caveat:** CI logs prove that query pack 1.12.0 was loaded, but the source snapshot above has not been proven byte-for-byte identical to that packaged query. Exact pack-source mapping and maintainer confirmation remain outstanding.

The inspected source contains a name-based branch for methods containing words such as `login`, `register`, and `edit`; `viewLogin` is excluded by the uninteresting `view` prefix. The branch sets source and sink to the same method. The prior zero-edge path-serialization explanation was only a hypothesis.

## 4. Differential self-edge experiment — 2026-11-20

A diagnostic-only fixture handler was added:

- `editSelfEdgeProbe()` is mapped to GET `/edit-self-edge`.
- It contains a syntactic self-call guarded by `Boolean.FALSE.booleanValue()`; this expression is always false, so the call is never executed.
- A MockMvc test confirms the route returns normally and does not recurse.
- The control is isolated under `tools/codeql-csrf-repro/`; it is not application code.

Exact fixture-only CodeQL evidence:
- Run [44672986192](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986192), job `136078077917`.
- Java 17.0.21+9; fixture Maven verify: 3 tests, all passed.
- The CodeQL log reports four path-graph errors: `editSelfEdgeProbe`, `edit`, `registerPage`, and `loginPage`.
- Fixture-only SARIF artifact [14601046000](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986192) was downloaded and parsed directly: SARIF 2.1.0, CSRF rule definition present, 0 CSRF results, `executionSuccessful: true`, and no error-level tool notifications.

The combined app + fixture job `136078078200` reports seven path-graph errors: the four fixture methods above plus the three application handlers. It still reports successful SARIF upload.

Combined artifact [14601135529](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44672986192) was downloaded and inspected:
- Raw SARIF: 11 results from other rules, 0 CSRF results.
- Post-processed/upload SARIF: 0 results.
- Both are SARIF 2.1.0; no corresponding error-level SARIF notification captures the path-graph failures.

**Interpretation:** the syntactic self-call control did not eliminate the exporter error. This weakens the simple claim that adding a source-level self-edge is sufficient to fix the zero-edge case. It does not prove whether CodeQL excludes that self-call from its call graph, whether the path graph has a broader node-identity issue, or whether the exporter is defective. Do not present the earlier zero-edge theory as a confirmed root cause and do not propose a self-edge patch as a fix without further query/graph evidence.

The fixture GET handlers return constants and do not mutate state. Name-based query matches in this fixture are diagnostic candidates, not proof of exploitable CSRF.

## 5. SARIF gate and application-security boundary

The fixture-only workflow continues to require the CSRF rule definition and at least one serialized CSRF result. On this exact head it finds the definition but zero results and deliberately fails closed. The fixture remains unpublished with `upload: never`.

Do not remove the gate just to make CodeQL green. Do not suppress `java/csrf-unprotected-request-type`, rename application handlers, weaken CSRF, or add a broad exemption.

The main application must continue to:
- keep Spring CSRF protection enabled for browser/session endpoints;
- keep the signed internal webhook exemption narrow;
- independently verify webhook HMAC, timestamp/replay handling, and idempotency;
- use unsafe HTTP methods for state-changing actions;
- retain CSRF regression tests.

## 6. Remaining acceptance gates

- [ ] Repository owner/security reviewer inspects published Code Scanning analyses and alerts; the current connector cannot access those endpoints.
- [ ] Confirm exact source revision for `codeql/java-queries@1.12.0`.
- [ ] Submit the sanitized fixture, logs, and SARIF to CodeQL maintainers and obtain review or a tested upstream fix.
- [ ] Re-run the exact fixture after any query/exporter fix and inspect raw plus post-processed SARIF.
- [ ] Independent browser-session CSRF tests for login/register/session lifecycle on isolated staging.
- [ ] Independent webhook HMAC replay/idempotency/concurrency review.
- [ ] Docker application-container runtime health smoke.
- [ ] W9/CI owner review of workflow changes.
- [ ] Verify main branch protection and required checks with a repository owner.

**Decision:** keep PR #62 Draft and Issue #13 open. Java CI passing does not resolve the CodeQL/SARIF blocker or grant security approval.
