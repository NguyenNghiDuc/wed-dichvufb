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

## Current differential result (2026-11-20)

On PR #62 head `279591718c495d88a9a747d832c0a381aa3a6725`, the fixture-only CodeQL job passed Maven verify (3 tests) but still logged path-graph errors for `editSelfEdgeProbe`, `edit`, `registerPage`, and `loginPage`. Its SARIF contains the CSRF rule definition but zero CSRF results, so the fail-closed gate fails. The self-call control did not resolve the error; the simple zero-edge explanation is therefore not confirmed.

The read-only GET handlers are heuristic candidates, not proof of CSRF exploitability. A path-graph error reproduces analyzer/export behavior; it does not establish an application vulnerability or an upstream root cause. Inspect exact SARIF notifications, query pack, CLI, Action, and source revision.

Do not suppress `java/csrf-unprotected-request-type`, rename handlers to evade its heuristic, or weaken CSRF to obtain a clean report. This fixture is evidence gathering, not a security approval.
