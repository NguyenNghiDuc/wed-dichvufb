# CodeQL CSRF SARIF investigation — Issue #13

**Status:** investigation only; not a security approval.  
**Checked:** 2026-11-23  
**Main:** `084f85dcaf1b39553298da18e021b7cbd688c990`  
**PR #62:** [Open · Draft](https://github.com/NguyenNghiDuc/wed-dichvufb/pull/62). Exact current head is recorded in §11; earlier SHAs and runs below are historical.

## 1. Historical exact-head CI evidence (2026-11-20)

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

The repository rulesets collection returned `[]`. The branch-protection endpoint returned HTTP 403 (`Resource not accessible by integration`), so this connection cannot verify branch-protection settings or required checks. Do not claim the branch is protected or unprotected from this response; owner verification remains a release blocker.

## 3. Query-source inspection and limits

The upstream CodeQL source snapshot previously inspected is commit
[`fe4820ab48ecd83a2ff590977f58beb27bc8ee10`](https://github.com/github/codeql/commit/fe4820ab48ecd83a2ff590977f58beb27bc8ee10):

- [CsrfUnprotectedRequestTypeQuery.qll](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll)
- [CsrfUnprotectedRequestType.ql](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/src/Security/CWE/CWE-352/CsrfUnprotectedRequestType.ql)
- [Upstream query test](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/test/query-tests/security/CWE-352/CsrfUnprotectedRequestTypeTest.java)

**Version caveat (superseded by the 2026-11-21 follow-up below):** the initial source snapshot was not yet mapped byte-for-byte to the loaded query pack. That mapping is now resolved across the CLI tags listed in §7.

The initial source review identified a same-method source/sink branch but did not yet establish why the path graph was empty. The exact query-source analysis and diagnostic QL-level self-edge test below supersede that preliminary interpretation.

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

**Correction (superseded by §7):** the Java self-call control was not a valid test of the QL path-graph edge relation. The official query's `relevantEdge` is not the raw Java call graph; it also requires `revFlow` to a database-update sink. The name-based branch excludes that sink path. Adding a Java self-call therefore does not add an edge to the official path graph. The earlier statement that this experiment weakened the zero-edge explanation was incorrect.

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
- [x] Map the upstream CSRF query source across CodeQL CLI 2.27.3, 2.27.4, and 2.27.5 tags; the relevant QLL/rule blobs are identical.
- [ ] Submit the sanitized fixture, logs, and SARIF to CodeQL maintainers and obtain review or a tested upstream fix.
- [ ] Re-run the exact fixture after any query/exporter fix and inspect raw plus post-processed SARIF.
- [ ] Independent browser-session CSRF tests for login/register/session lifecycle on isolated staging.
- [ ] Independent webhook HMAC replay/idempotency/concurrency review.
- [ ] Docker application-container runtime health smoke.
- [ ] W9/CI owner review of workflow changes.
- [ ] Verify main branch protection and required checks with a repository owner.

**Decision:** keep PR #62 Draft and Issue #13 open. Java CI passing does not resolve the CodeQL/SARIF blocker or grant security approval.

## 7. 2026-11-21 exact source mapping and differential QL probe

The upstream source has now been mapped across the CodeQL CLI release tags used by this investigation:

| CLI tag | Tag target commit | `CsrfUnprotectedRequestTypeQuery.qll` blob | Rule `.ql` blob |
|---|---|---|---|
| `codeql-cli/v2.27.3` | `a1575b98a8cc19bcfa7bb6cb41be688cb3dae65c` | `80d100d3d9e11b538720bfb177639148d73074b6` | `cf5c0b385ccf2ac006ba7befb930c705d9e4480a` |
| `codeql-cli/v2.27.4` | `1be292a1e54696ca543b4ad0f9b4537b9687caa7` | same | same |
| `codeql-cli/v2.27.5` | `ceeaafac4e629e0e39de04d40c719d7d18b4b612` | same | same |

The QLL and rule blobs are byte-identical across all three tags. This closes the earlier source-revision caveat for the query implementation observed in the CI packs.

- [Exact QLL at CLI 2.27.5](https://github.com/github/codeql/blob/codeql-cli/v2.27.5/java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll)
- [Exact rule at CLI 2.27.5](https://github.com/github/codeql/blob/codeql-cli/v2.27.5/java/ql/src/Security/CWE/CWE-352/CsrfUnprotectedRequestType.ql)
- [Upstream query test at CLI 2.27.5](https://github.com/github/codeql/blob/codeql-cli/v2.27.5/java/ql/test/query-tests/security/CWE-352/CsrfUnprotectedRequestTypeTest.java)

Relevant source logic:
- `sink` (lines 227–235) represents database-update methods.
- `revFlow` (lines 246–253) walks back from those sinks.
- `relevantEdge` (lines 259–260) requires a `CallGraph::edges` edge and `revFlow` at both ends.
- `unprotectedNameBasedStateChange` (lines 274–279) sets `sinkMethod = sourceMethod` and excludes database-update paths with `not unprotectedDatabaseUpdate(sourceMethod, _)`.
- The rule's path-graph `edges` predicate uses only `relevantEdge(pred, succ)`.

Therefore the name-based branch can select a source and sink that are the same method while declaring no edge that places that node in the path graph. This directly matches the exporter error `got start node that does not appear in graph`. The source-level cause is strongly established as a path-graph edge omission in the query design; upstream maintainer confirmation is still needed before calling it an accepted upstream defect or fix.

A diagnostic-only QL pack was added under `tools/codeql-csrf-repro/queries/`. It reuses the official state-change predicate but adds an explicit self-edge when `pred = succ and unprotectedStateChange(pred, succ)`. The fixture-only SARIF contains four results for this probe while the official query still emits zero results and logs the missing-start-node errors. This demonstrates that an explicit self-edge in the declared path graph is sufficient to serialize the four name-based same-node findings in this fixture.

Candidate upstream query correction (not applied to the official query or application):

```ql
query predicate edges(CallPathNode pred, CallPathNode succ) {
  relevantEdge(pred, succ)
  or
  pred = succ and unprotectedStateChange(pred, succ)
}
```

The Java self-call control remains as historical evidence but is not a test of the QL path-graph edge relation. The custom query is run only in the isolated fixture job with `upload: never`; it never publishes alerts.

## 8. 2026-11-21 exact-head CI evidence

Tested code head: `e00250286fab641bbc1c55ad3e43e731954faccd`  
Main: `084f85dcaf1b39553298da18e021b7cbd688c990`  
PR #62: Open, Draft; 11 commits ahead / 0 behind main.

### Java CI

[Run 44869100731](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869100731), job `136739355881`: **PASS**.
- Temurin Java 17.0.21.
- 173 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL 16.16 integration service.
- Flyway version guard passed for 16 versioned migrations.
- Maven `BUILD SUCCESS`.
- Production-profile JAR health smoke passed.
- Docker image build passed; application-container runtime health remains untested.

### CodeQL and SARIF

[Run 44869100874](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869100874): overall **FAILURE because the official-rule fail-closed gate correctly rejects zero serialized CSRF results**.

| Job | Job ID | Result |
|---|---:|---|
| Analyze Java security | `136739356615` | SUCCESS, but seven official CSRF path-graph errors and SARIF upload success |
| Isolated CSRF reproduction | `136739356847` | FAIL because the official rule still has zero serialized results; diagnostic self-edge probe passes |

Both jobs use the same pinned CodeQL CLI `2.27.5` and Java query pack `1.12.1`. The workflow pins the same Java bundle URL in both jobs, so the fixture job remains least-privilege without silently falling back to the Action-bundled CLI. Only the publishing job has `security-events: write`; the isolated fixture job has read-only `actions` and `contents` permissions and `upload: never`.

Fixture-only SARIF artifact [14693260893](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869100874):
- SARIF 2.1.0; CodeQL 2.27.5.
- Official rule definition present; official CSRF results: 0.
- Diagnostic rule `growlab/csrf-namebased-self-edge-probe` present; diagnostic results: **4**.
- `executionSuccessful: true` and zero error-level SARIF notifications.
- The log states `Diagnostic self-edge probe serialized 4 result(s).` before the official-rule gate fails.

Combined application + fixture SARIF artifact [14693375957](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869100874):
- Raw SARIF: 11 results from other rules; 0 official CSRF results.
- Post-processed/upload SARIF: 0 results.
- CodeQL 2.27.5; the log records seven missing-start-node path-graph errors (four fixture methods and three application methods).
- The combined job reports successful upload, but this is not evidence of complete CSRF serialization.

Toolchain alignment history:
- Commit `a9c9e5d749cf356233c9abdf9cec206249148b36` narrowed token permissions. Run 44868496142 then showed different default CLI versions between jobs because the read-only fixture could not obtain the server's default CLI feature flag (fixture 2.27.3 / pack 1.11.13; publishing job 2.27.5 / pack 1.12.1).
- Commit `c7bdbe880817e3c315b57db4fc877eae82319dcd` pinned the same 2.27.5 bundle URL in both jobs. Run 44868793383 confirmed both jobs used CLI 2.27.5 / pack 1.12.1 while retaining the least-privilege split.
- Commit `e00250286fab641bbc1c55ad3e43e731954faccd` added the QL-level self-edge diagnostic pack and fail-closed check.

## 9. Remaining acceptance gates

- [ ] CodeQL maintainer review of the same-node path-problem edge omission and diagnostic self-edge result; no upstream fix has been accepted yet.
- [ ] Repository owner/security reviewer inspects published Code Scanning analyses and alerts; the current connector cannot access those endpoints.
- [ ] Independent browser-session CSRF tests for login/register/session lifecycle on isolated staging.
- [ ] Independent webhook HMAC replay/idempotency/concurrency review.
- [ ] Docker application-container runtime health smoke.
- [ ] W9/CI owner review of workflow changes.
- [ ] Verify main branch protection and required checks with a repository owner; current integration receives 403 on the branch-protection endpoint.

**Decision:** keep PR #62 Draft and Issue #13 open. The path-graph omission is now supported by exact query source and a successful diagnostic self-edge SARIF experiment, but the official rule still fails to serialize its findings and no upstream maintainer/security review has been completed.
## 10. Exact current-head revalidation — 2026-11-21

The documentation-only follow-up head `b7a1ccc50ab64cd6d38fa09dbb4d512e3e76da46` was also tested before this final documentation refresh:

- Java CI [44869555373](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869555373), job `136740781486`: PASS; Java 17.0.21, 173 tests, PostgreSQL 16.16, Flyway version guard, JAR health smoke and Docker image build.
- CodeQL [44869555275](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44869555275), combined job `136740781151`: SUCCESS with seven official CSRF path-graph errors and successful upload.
- The same CodeQL run, isolated job `136740781340`: expected FAIL closed because official CSRF SARIF results remain zero; the diagnostic self-edge query again serialized four results.
- Exact-head fixture SARIF artifact `14693541550`: 4 diagnostic results, 0 official CSRF results, CodeQL 2.27.5 / SARIF 2.1.0.
- Exact-head combined artifact `14693631411`: 11 non-CSRF raw results, 0 official CSRF results, post-processed SARIF 0 results.

This re-run confirms the diagnostic result is stable on the current PR content. The commit after `b7a1ccc` only refreshes this report; it does not change application, workflow, or query code.

## 11. Exact-head SARIF integrity hardening — 2026-11-23

Tested code head: `6fa834a3ff837ed173c00fa65e1440266ca61d96`  
Main: `084f85dcaf1b39553298da18e021b7cbd688c990`  
PR #62: Open + Draft; 18 commits ahead / 0 behind main.

### Validator changes

- Commit `3c6f24cc11a27f2d7b0a02317805f184e7e0d1c7` strengthens `scripts/validate_codeql_csrf_sarif.py`: every SARIF run must contain invocation metadata; every invocation must explicitly set `executionSuccessful: true`; error-level `toolExecutionNotifications` and `toolConfigurationNotifications` reject the artifact.
- Commit `6fa834a3ff837ed173c00fa65e1440266ca61d96` adds four regression tests, bringing the validator suite to 10 tests.
- Local Python unit tests: 10/10 passed.
- Exact-head GitHub Java CI also ran the validator tests successfully.

This check is additional integrity validation. It does not interpret missing CodeQL path-graph log errors as SARIF notifications; those errors remain visible in the Actions logs and the official-result gate still blocks publication.

### Exact-head Java CI

[Run 45114995652](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45114995652), job `137495706599`: **PASS**.

- Temurin Java 17.0.21.
- SARIF validator: 10 tests passed.
- Flyway version-guard tests: 10 passed; 16 versioned migrations validated.
- PostgreSQL 16.16 integration service.
- Maven: 173 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- Production-profile JAR health smoke passed.
- Docker image build passed.
- JaCoCo artifact: `14791052919`.
- Docker application-container runtime health remains untested.

### Exact-head CodeQL and SARIF

[Run 45114995686](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45114995686): overall **FAILURE by design while the official CSRF result is absent**.

| Job | Job ID | Result |
|---|---:|---|
| Analyze Java security | `137495707012` | CodeQL analyze completed; validator failed on zero official CSRF results; SARIF upload was skipped |
| Isolated CSRF reproduction | `137495707258` | Official CSRF result remains zero; diagnostic QL probe emits four results; fixture upload disabled |

Both jobs use CodeQL Action v4.39.1 at immutable SHA `9b3254085494b55a0f7c4c2b3d2d52cc712ce370`, CLI 2.27.5, Java query pack 1.12.1, and Java 17.

Downloaded and parsed exact-head artifacts:

- Combined artifact [14791745649](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45114995686): raw SARIF has 11 non-CSRF results and 0 official `java/csrf-unprotected-request-type` results; post-processed SARIF has 0 results.
- Fixture artifact [14790862737](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45114995686): 4 diagnostic `growlab/csrf-namebased-self-edge-probe` results and 0 official CSRF results.
- Both artifacts are SARIF 2.1.0, with one invocation each, `executionSuccessful: true`, and zero error-level SARIF notifications.
- The combined CodeQL log still reports seven missing-start-node errors; the isolated fixture log reports four. The errors are not represented as SARIF error-level notifications.

The strengthened validator was run directly against both downloaded artifact ZIPs. It accepted their invocation metadata but rejected both because the official CSRF rule has zero serialized results. The combined workflow skipped the upload step; the isolated job remains `upload: never`.

### Current decision and remaining gates

The validator hardening is verified. The official CodeQL query/exporter problem is **not fixed** and no upstream maintainer acceptance exists. The diagnostic self-edge result remains evidence for investigation, not a replacement for official CSRF results and not proof that the read-only GET fixture is exploitable.

- [ ] CodeQL maintainer review or tested upstream correction.
- [ ] Repository owner/security reviewer inspects published Code Scanning analyses and alerts.
- [ ] Independent browser-session CSRF tests on isolated staging.
- [ ] Independent webhook HMAC replay/idempotency/concurrency review.
- [ ] Docker application-container runtime health smoke.
- [ ] W9/CI owner review.
- [ ] Repository owner verifies main branch protection and required checks.

**Decision:** keep PR #62 Draft and Issue #13 open. No application source, Spring CSRF settings, migrations, or production behavior changed in this follow-up. No production deployment, real transaction, artificial engagement, secret use, force push, or direct main write.

## 12. 2026-11-24 — correct zero-finding semantics and inspect path-graph logs

### Fresh main and PR baseline

- Main ref was re-read from GitHub: `084f85dcaf1b39553298da18e021b7cbd688c990`.
- Latest scheduled main CodeQL run: [45176815118](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45176815118), job `137700088488`, conclusion **SUCCESS**. The decoded log nevertheless contains three errors for `edit`, `registerPage`, and `loginPage`, followed by SARIF upload success. This is the same false-green condition Issue #13 tracks.
- Latest main Java CI run: [39548204218](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/39548204218), job `119051109194`, **PASS** (Java 17, PostgreSQL 16.15, 173 tests, JAR smoke, Docker build). It is an older baseline from 2026-10-21; no newer main Java CI run was present in the inspected run list.
- Previous PR #62 exact head: `2bf7c0d263d863763e9f11a4cbb4e3b15c6bc625`; Java CI [45115539080](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45115539080) passed, while CodeQL [45115539033](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45115539033) failed closed. Combined job `137497416458` logged seven missing-start-node errors; fixture job `137497416127` logged four.
- Combined SARIF artifact [14791077843](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45115539033) was downloaded and inspected. It contains 11 non-CSRF results, zero official CSRF results, and 207 `toolExecutionNotifications` (203 `none`, four `note`, zero error-level). The path-graph errors exist only in the CLI log and are not represented as error-level SARIF notifications. Fixture artifact [14791731803](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/45115539033) contains four diagnostic self-edge results and zero official CSRF results.

### Corrected acceptance semantics

The previous validator incorrectly required at least one result for the official CSRF rule. A security query returning zero findings is valid when the rule was executed successfully and no finding exists; requiring a non-zero result could force false-positive findings just to make CI green. The official rule must therefore have a SARIF definition, not a fabricated minimum finding count.

The new design keeps the security gate fail-closed without treating zero findings as failure:

1. `validate_codeql_csrf_sarif.py --require-rule-definition` requires the official CSRF rule definition, SARIF 2.1.0, invocation metadata, explicit `executionSuccessful: true`, and no error-level SARIF notifications. Zero official findings are allowed.
2. After CodeQL analyze, the workflow locates the exact CLI/database reported by the analyze step and re-runs `codeql database interpret-results` against the persisted BQRS results, capturing stdout and stderr.
3. `check_codeql_path_graph_log.py` fails the job if the captured interpretation log contains any `ERROR: While processing path-problem rule java/csrf-unprotected-request-type` entry. This checks the failure channel that the SARIF notification validator cannot see.
4. The isolated fixture still requires the diagnostic `growlab/csrf-namebased-self-edge-probe` result, but it never uploads fixture alerts. The combined job uploads raw SARIF only after the path-graph log and SARIF metadata gates pass.

The path-graph re-interpretation step is deliberately duplicated in the combined and isolated jobs so both the application database and clean-room fixture are checked. If the CLI binary or persisted query results cannot be located, the job fails closed rather than silently skipping the check.

### Scope and local regression evidence

- No application controller, Spring CSRF setting, migration, payment behavior, or production configuration changed.
- No rule is suppressed, no handler is renamed, and no self-edge is injected into the official query.
- Local Python regression suite for the new validator semantics and path-graph log parser: **17 tests passed**.
- The actual combined SARIF artifact passes the rule-definition-only check (zero findings allowed); a captured representative path-graph log fails the new parser as intended.
- GitHub Actions must still validate the exact pushed head. New workflow run IDs and results are recorded in the PR after completion; local tests do not substitute for that run.

**Decision:** PR #62 remains Draft and Issue #13 remains open. Do not merge while the exact-head path-graph re-interpretation gate, Java/PostgreSQL/Flyway/JAR/Docker checks, and independent security review are incomplete.
