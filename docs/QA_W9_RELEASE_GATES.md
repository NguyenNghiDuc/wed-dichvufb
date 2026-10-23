# W9 — GrowLab cross-module quality gates (Issue #46)

## Ownership and release conditions
W9 modifies only `.github/workflows/`, cross-module `src/test/` and `docs/QA_*.md`. No application services, models, Flyway migrations or production credentials are changed. This PR remains draft until real CI results, review and staging acceptance are available.

## Automated coverage
- `W9CustomerJourneyIntegrationTest` (H2, MockMvc with real form-login session, **not** `@WithMockUser`): register two accounts, verify BCrypt + default role, log in, create campaign with CSRF, follow status and history, create support ticket with CSRF, view it and verify that the second account cannot access/read/reply to another owner's campaign or ticket (404).
- Negative coverage: anonymous redirects; user not authorized for Admin; missing CSRF on registration, campaign and support POST; invalid username, wrong campaign platform/host, invalid JSON and short ticket.
- `PostgresW9CancellationRaceTest` (`@ActiveProfiles("prod")`, real PostgreSQL 16): two concurrent cancellation requests race for the same campaign. Exactly one succeeds and the history contains only the creation and one cancellation record.
- Existing CI Flyway collision guard and PostgreSQL integration tests are kept. **A branch-local PASS cannot prove that PR #33, #35 and #37 migrations are mutually compatible.** See #41 and W7 ownership.

## Java CI / runtime gates
1. Python Flyway-guard unit tests + duplicate-version check.
2. Java 17 + Maven `clean verify`; test XML/text and JaCoCo artifacts uploaded with `if: always()` for diagnosing failures.
3. JAR build, production profile startup, `/actuator/health`, index and registration GET smoke checks.
4. Docker **build + run**, production profile connecting to CI PostgreSQL through host network, wait for health and verify login/index endpoints. Container logs are shown on failure; container is removed on exit.
5. Independent CodeQL workflow must finish with `success`; `queued`, `in_progress` and `failure` are not green.

## Remaining manual QA / blockers
- Test actual mobile browser/keyboard, Safari and Chrome cookie handling, forwarded HTTPS proxy settings, and real login pages. MockMvc and local host-network Docker do not replace staging UAT.
- Verify all relevant PR heads **after** the final commit and all status checks. Other teams own business defects; W7 owns SQL/renumbering. #41 remains a blocker for simultaneous integration until combined Flyway tests are proven.
- Consider dedicated isolated database for concurrency/load and aggressive fault-injection; this suite checks only a two-request race.
- Never disclose full credentials or webhooks from logs, never merge/redploy based on a queued run, and do not interpret CI PASS as full production security certification.

## How to run in CI environment
Run `mvn -B --no-transfer-progress clean verify` against the configured PostgreSQL 16 service on localhost:5432 (isolated CI only; login `growlab`, password `testpass` already defined in workflow). Use `mvn -B -Dtest=W9CustomerJourneyIntegrationTest test` for the H2 journey alone. The production-profile PostgreSQL test requires a live CI PostgreSQL service and cannot pass against H2.

**Test evidence**: record GitHub Actions run URLs, head SHA, job conclusions and failing steps in Issue #46. Local/static code review is not CI evidence.


## Additional W9 regression gate (18 Oct 2026)

- `W9SessionRolesAndDuplicateTest`: actual registration/login + logout CSRF and session invalidation, USER/STAFF/ADMIN route matrix, duplicate cancellation with one audit transition, XSS stored ticket displayed as **escaped text**, invalid JSON and bounded API pagination. Its role-matrix requests intentionally use test principals to isolate authorization policy; login/logout and tenant flows use **real sessions**.
- `java-ci.yml` parses real `target/site/jacoco/jacoco.xml`, fails if absent or measuring zero covered application lines and writes the **measured**, not invented, line coverage to GitHub step summary. No arbitrary minimum % threshold has been claimed or set.
- `w9-integration-rehearsal.yml` is a **manual read-only runner simulation**: supply immutable HEAD SHAs for PRs #33, #35 and #37; verify each matches the live PR, simulate merges on an ephemeral branch, reject duplicate Flyway versions then run Maven `clean verify` against PostgreSQL 16. It never pushes, modifies production, repairs Flyway history or approves merges. **Workflow dispatch availability depends on this workflow being present on the default branch; this draft PR alone does not establish a runnable dispatch.** Current combined-migration verification remains **NOT EXECUTED**.

## Cross-PR overlap snapshot — 18 Oct 2026 (not exhaustive of every dependency PR)

| PRs | Shared paths / reason for coordination |
| --- | --- |
| #22 / #37 / #48 | `.github/workflows/java-ci.yml` — W1/W6/W9 edits must be reconciled before merge |
| #31 / #35 | `src/main/resources/static/style.css` |
| #34 / #40 | `ContentDraftService.java` and `content_calendar.html` |
| #35 / #36 | `admin_support_detail.html`, `admin_support_list.html` |
| #35 / #44 | Seven support/chat documentation files; W3's PR #44 also holds proposed V20–V25 migrations |
| #37 / #49 | `docs/DEPLOYMENT.md` |
| #42 / #43 | `HelpController.java`, `help.html` — duplicated FAQ feature |

### Flyway coordination (Issue #41)
At inspection, PR #33 adds V10–V13, PR #35 V10–V15, and PR #37 V10–V12 with **different SQL**. The independent PR #44 proposes V20–V25 for W3 but is not proof PR #35 includes the renumbering. W7 must establish a unique final plan and prove database upgrade safety **before** any combined release. Collision file names need not be identical to break Flyway.

### CodeQL analyzer issue (Issue #13)
Verified job https://github.com/NguyenNghiDuc/wed-dichvufb/actions/runs/39093099906 returned `success`, while the actual log showed `java/csrf-unprotected-request-type` internal path graph errors for `edit`, `registerPage`, `loginPage`. Therefore a green CodeQL status is necessary but **not sufficient** for CSRF analysis acceptance. Keep #13 open for SARIF/analyzer investigation and independent staging CSRF verification by W1/W9.

### Evidence taxonomy
- **Verified:** commit SHA obtained from GitHub, code changes present, PR file overlaps identified, CodeQL error text observed in a completed log.
- **Pending:** Java 17 full Maven build, PostgreSQL race, JaCoCo metrics, production JAR, Docker runtime, new CI gates on the current W9 HEAD (check each Actions run's actual conclusion and jobs).
- **Blocked:** actual multi-PR PostgreSQL/Flyway verification pending W7's migration version resolution and runnable integration-rehearsal workflow.
