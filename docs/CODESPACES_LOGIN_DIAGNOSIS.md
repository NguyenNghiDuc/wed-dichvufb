# Codespaces: Spring Boot Whitelabel 403 at /login

This document deals specifically with a GitHub Codespaces forwarded-port URL such as
`https://<codespace>-8000.app.github.dev/login`. The Whitelabel 403 page only confirms
that some Spring Boot application responded; it does **not** prove which branch/version
or application process served the request. Do not disable CSRF or permit all paths to
avoid the error.

## 1. Check the local process, without revealing credentials

From the repository root in the **same Codespace** as the browser link:

```bash
git status --short
git branch --show-current
git rev-parse --short HEAD
bash scripts/diagnose-codespaces-login.sh 8000
```

Check that the branch is `fix/login-stale-session-403` and its commit matches the
latest GitHub PR #22 head. The diagnostic script checks the listener and two local
requests, without printing cookies, credentials, process command lines or env vars.

For the fixed application, the direct request must be HTTP 200 and have:
`X-GrowLab-Auth-Revision: session-recovery-v2`.
The reverse-proxy simulation should also return 200.

## 2. Interpret the results

| Local direct test | Simulated proxy test | Browser | Most useful next action |
|---|---|---|---|
| Not connected | Not connected | Error | Start the actual Spring Boot app on port 8000. |
| 403 or missing revision header | 403 or missing header | 403 | Inspect which process owns port 8000; old app/build or incorrect checkout may be serving requests. |
| 200 + marker | 403 | 403 | Check proxy-header handling and process logs. |
| 200 + marker | 200 + marker | 403 | Check the Codespaces **Ports** tab, visibility/authentication, exact forwarded URL, browser cookies/incognito and browser's Network tab. |
| 200 + marker | 200 + marker | 200 | Login page is reachable. Test POST login with a real test user, logout, then protected pages. |

`ss -ltnp '( sport = :8000 )'` or `lsof -nP -iTCP:8000 -sTCP:LISTEN`
can help identify the port owner. **Do not kill unrelated processes.**

## 3. Apply the current branch safely

If `git status --short` shows local changes, save them before switching branches;
do not discard them. Then:

```bash
git fetch origin
git switch fix/login-stale-session-403
git pull --ff-only origin fix/login-stale-session-403
```

Stop the existing app by pressing Ctrl+C in **the terminal where it is running**.
Wait until port 8000 is free, then:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8000
```

Open a **second** terminal to run the diagnostic script. Once it reports OK,
open the exact Codespaces URL at `/login` in a private browsing window.

## 4. If local probes show 200 but the forwarded browser still shows 403

Inspect the Codespaces **Ports** panel. Confirm that **port 8000** belongs to the
active Codespace and forwards HTTP to the intended process. If port visibility is
private, browser access may require an authenticated GitHub session; do not
automatically expose a service publicly just to work around an auth failure.
Also check DevTools > Network > /login for the response status and
`X-GrowLab-Auth-Revision` header. Compare the results with localhost.

If local and remote disagree, collect the script output, the Network response
status/headers (remove any cookies or tokens first), and the application logs
around the failed request. GitHub connector access cannot restart Codespaces
processes or inspect private browser sessions.

## 5. Security invariants

- /login, /register, and /error remain public where intended.
- /admin/** remains ADMIN only; /staff/** requires assigned role.
- State-changing requests require CSRF except verified internal payment webhook POST.
- Revoked user accounts cannot access protected pages with old sessions.
- The revision header identifies the running build behavior, not a secret token.
- This local diagnostic provides evidence; it is not proof of end-to-end production readiness.
