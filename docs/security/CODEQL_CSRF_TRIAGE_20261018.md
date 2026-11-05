# CodeQL CSRF SARIF triage — 2026-10-18

Tracking: #13; roadmap gate: #3.

## Known evidence

CodeQL Java `java/csrf-unprotected-request-type` emitted `got start node that does not appear in graph` for `AuthController.loginPage` and `registerPage` in multiple runs. A successful workflow conclusion does not establish complete query coverage.

## Required investigation

1. Inspect published Code Scanning alerts and download the SARIF artifact for the affected run. Check whether results for the affected paths are absent or partial.
2. Record CodeQL CLI and action versions, Java 17 version, query pack, run ID and exact exporter logs. Reproduce on a minimal controller with GET login/register handlers.
3. On an isolated staging environment, verify session cookie attributes and CSRF token issuance, login/register POST with missing/invalid token (must reject), valid token (must follow expected authentication/validation flow), and the narrowly exempted signed webhook with invalid signature, timestamp and replay attempts (must reject).
4. Distinguish application vulnerability from analyzer/export defect; fix confirmed vulnerabilities, otherwise report upstream with a minimal reproduction. Do not suppress the query to turn CI green.
5. Attach security review, PostgreSQL/Flyway integration results, Java tests, production JAR and Docker checks, and owner UAT evidence before any production approval.

## Release gate

This document is a checklist, not evidence that any security test passed. Keep issue #13 open and security approval blocked until findings are classified and independently reviewed. Do not use real credentials, payments or production deployments for reproduction.
