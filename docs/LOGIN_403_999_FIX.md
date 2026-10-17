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
- Invalid authenticated sessions are invalidated and continue as anonymous, preserving
  proper authentication checks on protected endpoints (fix/login-stale-session-403).
- Keep CSRF required everywhere except the signed webhook POST route.
- Regression tests check error dispatch, favicon, admin access and redirects.

## Manual Codespaces verification
1. In the Codespaces terminal, switch to the tested fix branch.
2. Stop any older Spring Boot processes on port 8000, then restart the app.
3. Confirm the forwarded port has protocol HTTP and appropriate visibility.
4. Test /login, /favicon.ico, /favicon.svg, /error?continue, /campaigns (logged-out),
   and POST /login with a known staging account.
5. Test with old browser session cookie and fresh incognito session.

If a port proxy still blocks requests, inspect the forwarded-port authorization and
restart Codespaces. App-side changes cannot repair GitHub proxy permissions.
