# Isolated CodeQL CSRF name-heuristic reproduction

This is a separate Maven/Spring Boot project used only by Issue #13. It is not a Maven module of GrowLab and is never included in the application JAR or Docker image.

## Scope

- Spring Boot 3.2.5 / Java 17.
- Read-only GET handlers named `loginPage`, `registerPage`, and `edit`.
- Negative control `viewLogin`.
- Diagnostic-only `editSelfEdgeProbe` control: a syntactic self-call is guarded by `Boolean.FALSE.booleanValue()`, which is always false. It is never executed and exists only to test whether an explicit call-graph self-edge changes the path-graph export behavior.
- A POST control protected by Spring Security's default CSRF filter.
- No database, network integration, real account, credential, payment, or production data.

The controller deliberately mirrors the relevant mapping/name shape without importing the GrowLab application. The tests prove that the fixture's GET endpoints remain read-only and that an unsafe POST rejects a missing CSRF token while accepting a test token. The diagnostic self-edge route must return normally without recursion.

## Local reproduction

From the repository root, with JDK 17 and Maven 3.8+:

```sh
mvn -B --no-transfer-progress -f tools/codeql-csrf-repro/pom.xml verify
```

The CodeQL workflow runs this independent Maven build after compiling the main application, within the same CodeQL manual-build tracing window. The workflow also retains both raw CLI SARIF and post-processed/upload-ready SARIF as a short-lived Actions artifact for Issue #13 review.

## Interpretation

The baseline handlers test the name-based heuristic without an interprocedural self-edge. The diagnostic control adds a syntactic self-edge that is unreachable at runtime. If only that control avoids the path-graph error, it supports the zero-edge hypothesis; if it fails identically, the hypothesis is weakened and further graph inspection is required.

A CodeQL path-graph error for any fixture handler reproduces exporter/query behavior outside GrowLab application code. It does not by itself prove an upstream defect or a CSRF vulnerability. Inspect exact SARIF notifications, query pack, CLI, Action, and source revision.

Do not suppress `java/csrf-unprotected-request-type`, rename handlers to evade its heuristic, or weaken CSRF to obtain a clean report. This fixture is evidence gathering, not a security approval.
