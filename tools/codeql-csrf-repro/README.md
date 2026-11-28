# Isolated CodeQL CSRF name-heuristic reproduction

This is a separate Maven/Spring Boot project used only by Issue #13. It is not a Maven module of GrowLab and is never included in the application JAR or Docker image.

## Scope

- Spring Boot 3.2.5 / Java 17.
- Read-only GET handlers named `loginPage`, `registerPage`, and `edit`.
- Negative control `viewLogin`.
- Diagnostic-only `editSelfEdgeProbe`: a syntactic self-call is guarded by `Boolean.FALSE.booleanValue()`, which is always false and never executes. It tests whether a self-call in source is enough to change path-graph serialization.
- A POST control protected by Spring Security's default CSRF filter.
- No database, network integration, real account, credential, payment, or production data.

The tests prove that the fixture GET endpoints return normally, including the diagnostic route, and that an unsafe POST rejects a missing CSRF token while accepting a test token.

## Local reproduction

From the repository root, with JDK 17 and Maven 3.8+:

```sh
mvn -B --no-transfer-progress -f tools/codeql-csrf-repro/pom.xml verify
```

The CodeQL workflow runs this independent Maven build in a separate database job with upload disabled. The combined analysis also compiles the application and fixture and retains raw plus post-processed SARIF for Issue #13 review.

## Current differential result (2026-11-21)

The official query source has a same-node name-based branch (`sinkMethod = sourceMethod`) but the path-graph `edges` relation only exposes `relevantEdge`, which requires a database-update `revFlow`. The name-based branch explicitly excludes database-update paths. This leaves the selected same-node finding out of the declared path graph and matches the CodeQL missing-start-node export error.

A diagnostic query pack in `queries/` adds a self-edge for `pred = succ and unprotectedStateChange(pred, succ)`. On PR #62 head `e00250286fab641bbc1c55ad3e43e731954faccd`, CodeQL run 44869100874 emitted four SARIF results for `growlab/csrf-namebased-self-edge-probe`; the official `java/csrf-unprotected-request-type` still emitted zero and the fail-closed gate failed. This is strong evidence for the path-graph edge omission, not proof that the read-only GET fixture handlers are exploitable.

The earlier Java self-call control is not equivalent to a QL path-graph self-edge and is not treated as a counterexample. The diagnostic query runs only in the isolated fixture job with `upload: never`; it never publishes alerts.

The CodeQL workflow pins the same 2.27.5 Java bundle in both jobs. Only the publishing job has `security-events: write`; the isolated job remains read-only. Java CI run 44869100731 passed 173 tests on Java 17.0.21 with PostgreSQL 16.16, Flyway version guard, JAR health smoke, and Docker image build.

The read-only GET handlers are heuristic candidates, not proof of CSRF exploitability. Do not suppress `java/csrf-unprotected-request-type`, rename handlers to evade its heuristic, or weaken CSRF to obtain a clean report. This fixture is evidence gathering, not a security approval.