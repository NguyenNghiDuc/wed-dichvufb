# Fix Codespaces login redirects to /error?continue with status 999

## Root cause
Spring Boot's error dispatch to /error was caught by Spring Security as a protected URL,
and could be cached as a return destination for the login flow. A missing favicon.ico
can trigger the same sequence. A stale authenticated session also previously produced
a direct 403 when opening public /login.

## What was fixed
- Allow /error and DispatcherType.ERROR to render the actual application error, without
  turning it into a pending login request. Authorization of protected endpoints is unchanged.
- Always land on /campaigns after successful form login to avoid legacy /error saved requests
  in sessions. Deep links will no longer automatically resume after login.
- Provide an SVG favicon, and redirect legacy /favicon.ico requests to it.
- Public GET /login remains accessible with stale cookies. On a private GET, revoked sessions
  are invalidated and redirected to /login?sessionExpired; unsafe methods return 401.
  Protected-route authorization is still enforced (fix/login-stale-session-403).
- Keep CSRF required everywhere except the signed webhook POST route.
- Regression tests check error dispatch, favicon, admin access and redirects.

## Codespaces verification (not yet manually accepted)
The fix is on a draft pull request, not deployed to Codespaces by a GitHub commit.
Open the Codespaces terminal and run:

```bash
git fetch origin
git switch fix/login-stale-session-403
git pull --ff-only origin fix/login-stale-session-403
PORT=8000 bash scripts/codespaces-login.sh check
```

If this reports the old revision, 403, or cannot connect, inspect the listener:
`ss -ltnp | grep ':8000'`. Stop the older Java process **after checking its PID**.
Then start the new build (it does not kill an existing server automatically):

```bash
PORT=8000 bash scripts/codespaces-login.sh run
```

In a separate terminal run `PORT=8000 bash scripts/codespaces-login.sh check`.
A pass requires local `GET /login` = HTTP 200 and the response header
`X-GrowLab-Auth-Revision: session-recovery-v2`. For port 8080 use `PORT=8080`.

If the local check succeeds but the browser's `*.app.github.dev` forwarded
URL still returns 403, inspect GitHub Codespaces port forwarding visibility,
authentication and stale browser cookies; it may be a GitHub proxy denial rather
than a Spring Security response. Validate /favicon.ico, /favicon.svg,
/error?continue, logged-out /campaigns, login, CSRF, and Admin permissions in a
real browser before approving a merge. CodeQL issue #13 also remains open.
