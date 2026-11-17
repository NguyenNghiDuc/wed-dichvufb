# Isolated CodeQL CSRF name-heuristic reproduction

This is a separate Maven/Spring Boot project used only by Issue #13. It is not a Maven module of GrowLab and is never included in the application JAR or Docker image.

## Scope

- Spring Boot 3.2.5 / Java 17.
- Read-only GET handlers named `loginPage`, `registerPage`, and `edit`.
- Negative control `viewLogin`.
- A POST control protected by Spring Security's default CSRF filter.
- No database, network integration, real account, credential, payment, or production data.

The controller deliberately mirrors the relevant mapping/name shape without importing the GrowLab application. The test proves that the fixture's GET endpoints are read-only and that an unsafe POST rejects a missing CSRF token while accepting a test token.

## Local reproduction

From the repository root, with JDK 17 and Maven 3.8+:

```sh
mvn -B --no-transfer-progress -f tools/codeql-csrf-repro/pom.xml verify
```

The CodeQL workflow runs this independent Maven build after compiling the main application, within the same CodeQL manual-build tracing window. The workflow also retains both raw CLI SARIF and post-processed/upload-ready SARIF as a short-lived Actions artifact for Issue #13 review.

## Interpretation

A CodeQL path-graph error for one of these fixture handlers would reproduce the exporter/query behavior outside the GrowLab application code. It would not by itself prove an upstream defect: the exact SARIF notifications, query pack, CLI, Action, and source revision must still be inspected. A clean run would instead narrow the cause toward application-specific code or build extraction.

Do not suppress `java/csrf-unprotected-request-type`, rename handlers to evade its heuristic, or weaken CSRF to obtain a clean report. This fixture is evidence gathering, not a security approval.
