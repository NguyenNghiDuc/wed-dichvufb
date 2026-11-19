# CodeQL CSRF path-graph analysis — Issue #13

**Status: investigation evidence; not a security approval.**  
**Checked:** 2026-11-19  
**Repository main:** `084f85dcaf1b39553298da18e021b7cbd688c990`  
**Investigation PR:** #62, head `b5fffd901d99394c7887ee0df9ee204979654b3b` (Draft)

## 1. Exact-run evidence

The last completed exact-head runs for PR #62 are:

| Workflow | Run / job | Result |
|---|---|---|
| Java CI | [44237448945](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44237448945) / `134576886167` | PASS |
| CodeQL combined app + fixture | [44237448955](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44237448955) / `134576886246` | Analyze step SUCCESS; six path-graph export errors |
| CodeQL fixture-only | [44237448955](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44237448955) / `134576885915` | Expected FAIL from the fail-closed SARIF gate |

Toolchain from the job logs:

- Java: Temurin 17.0.21+9
- CodeQL Action: v4.39.1, immutable action commit `9b3254085494b55a0f7c4c2b3d2d52cc712ce370`
- CodeQL CLI: 2.27.4
- Java query pack loaded by the runner: `codeql/java-queries/1.12.0`

The isolated fixture job logs exactly three errors for `java/csrf-unprotected-request-type`:

- `edit`
- `registerPage`
- `loginPage`

The combined application + fixture job logs the same three names twice. The analyze action still exits successfully and uploads SARIF.

Artifacts from run `44237448955`:

- [Fixture-only SARIF artifact 14398665233](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44237448955)
- [Combined raw and post-processed SARIF artifact 14398610680](https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/44237448955)

The fixture SARIF is 2.1.0, includes the query definition (in a tool extension), has zero results for the CSRF rule, reports `executionSuccessful: true`, and has no corresponding error-level SARIF notification. The combined raw SARIF contains 11 results from other rules and zero CSRF results; the post-processed PR SARIF contains zero results. The fixture job uses `upload: never`, so it does not publish test alerts.

## 2. Query-source inspection

The upstream CodeQL source snapshot inspected for this analysis is commit
[`fe4820ab48ecd83a2ff590977f58beb27bc8ee10`](https://github.com/github/codeql/commit/fe4820ab48ecd83a2ff590977f58beb27bc8ee10):

- [CsrfUnprotectedRequestTypeQuery.qll](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/lib/semmle/code/java/security/CsrfUnprotectedRequestTypeQuery.qll)
- [CsrfUnprotectedRequestType.ql](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/src/Security/CWE/CWE-352/CsrfUnprotectedRequestType.ql)
- [Upstream query test](https://github.com/github/codeql/blob/fe4820ab48ecd83a2ff590977f58beb27bc8ee10/java/ql/test/query-tests/security/CWE-352/CsrfUnprotectedRequestTypeTest.java)

**Version caveat:** the run log proves that pack 1.12.0 was loaded. The source links above are a separately identified upstream repository snapshot; this note does not claim that commit is byte-for-byte the exact pack source revision. Confirm the corresponding pack source revision with the CodeQL maintainers before treating this as an upstream root-cause report.

Relevant query logic in the inspected source:

1. `NameBasedStateChangeMethod` matches method names containing words such as `login`, `register`, and `edit`.
2. Its uninteresting-prefix exclusion includes `get`, `show`, `view`, `list`, `query`, and `find`.
3. `unprotectedNameBasedStateChange(sourceMethod, sinkMethod)` requires `sinkMethod = sourceMethod`. This is a same-method, zero-interprocedural-edge result; it does not require a database update.
4. The path-problem wrapper exposes only `relevantEdge(pred, succ)`.
5. `relevantEdge` requires a real `CallGraph::edges(pred, succ)` edge (and both nodes to be in reverse flow).

The isolated fixture is deliberately shaped to exercise that branch:

| Fixture method | Why it matches / does not match |
|---|---|
| `loginPage` | Contains the configured interesting word `login` |
| `registerPage` | Contains `register` |
| `edit` | Is itself an interesting word |
| `viewLogin` | Negative control; begins with the uninteresting prefix `view` |

These fixture GET methods return constant strings and do not update a database. Their purpose is to exercise the name-based heuristic, not to represent a confirmed CSRF vulnerability.

## 3. Working diagnosis — zero-edge path serialization

**Evidence-supported hypothesis, not yet upstream-confirmed:** the name-based branch can produce a path-problem result whose source and sink are the same method node, while the exported path graph contains only actual call-graph edges. A zero-edge path therefore has no edge from which the SARIF exporter can materialize its start node. That is consistent with the exact exporter message:

```text
got start node that does not appear in graph: edit
got start node that does not appear in graph: registerPage
got start node that does not appear in graph: loginPage
```

This explains the observed combination:

- the query is interpreted;
- the exporter logs a path-graph error;
- SARIF omits the CSRF results and omits the corresponding error notification;
- the invocation still says `executionSuccessful: true`;
- the CodeQL Action reports upload success.

A possible upstream remedy is to make same-node findings representable in the path graph (for example, provide an explicit self-edge for the name-based branch), or to use a result kind/graph design that does not require a non-empty interprocedural path. This is only a proposal for maintainer review; no upstream query code is changed here.

The official query help describes this rule as detecting state-changing actions exposed through HTTP request types not protected by default from CSRF. The fixture's name-only matches are heuristic candidates, not evidence that its read-only GET handlers are exploitable.

## 4. Why the fail-closed gate remains

The fixture-only job currently requires the expected CSRF rule definition and at least one serialized CSRF result. On the observed exact head, it fails because the definition exists but zero results are serialized. This deliberately prevents the repository from treating a successful analyze/upload message as proof that the query path was preserved.

Do not remove this gate merely to make CodeQL green. If maintainers confirm a fixed exporter/query, rerun the exact fixture and inspect the actual SARIF before changing the acceptance condition. Keep fixture upload disabled.

## 5. Application-security boundary

No application CSRF configuration was changed in PR #62. The main application must continue to:

- keep Spring CSRF protection enabled for browser/session endpoints;
- keep the internal signed webhook exception narrow and separately verify HMAC, timestamp/replay, and idempotency;
- use unsafe HTTP methods for state-changing actions;
- retain the existing CSRF regression tests.

Do not rename handlers, suppress the query, weaken CSRF, or add a broad CSRF exemption to silence the analyzer.

## 6. Remaining acceptance gates

- [ ] Repository owner/security reviewer inspects published Code Scanning analyses and alerts.
- [ ] Confirm the exact Java query-pack 1.12.0 source revision and submit the sanitized reproduction upstream.
- [ ] Obtain upstream maintainer response or a tested query-pack fix; rerun the fixture and inspect SARIF.
- [ ] Independent browser-session CSRF tests for login/register/session lifecycle on isolated staging.
- [ ] Independent webhook HMAC replay/idempotency/concurrency review.
- [ ] Docker application-container runtime health smoke (image build and JAR smoke alone do not cover this).
- [ ] W9/CI owner review of the workflow changes.
- [ ] Verify main branch protection and required checks with a repository owner; the available connection could list no repository rulesets but could not read the branch-protection endpoint.

Java CI on this PR head passed 173 tests on Java 17, PostgreSQL 16.16/Flyway V1–V16 and the V11→V16 upgrade path, production-profile JAR health smoke, and Docker image build. That does not satisfy the outstanding security and runtime gates.

**Decision:** keep PR #62 Draft; do not merge main or approve production.
