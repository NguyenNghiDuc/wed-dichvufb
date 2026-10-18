# W9 — GrowLab cross-module quality gates (Issue #46)

## Ownership and release conditions
W9 modifies only `.github/workflows/java-ci.yml`, `src/test/`, and `docs/QA_W9_RELEASE_GATES.md`. No application services, models, Flyway migrations or production credentials are changed. This PR remains draft until real CI results, review and staging acceptance are available.

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
