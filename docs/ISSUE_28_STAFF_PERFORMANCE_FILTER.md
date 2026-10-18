# Issue #28 — exact STAFF progress history by actor

Admin `/admin/staff/performance?actor=<username>` now supports exact actor search for a single person's logged start/completion events within the last 30 days, regardless of the top-20 overall ranking. The same filter is available at `/api/v1/admin/staff/performance` and `/admin/staff/performance.csv`; the staff directory provides a direct link per account.

Input is strictly validated before entering a JDBC prepared statement (`[a-zA-Z0-9._-]{3,100}`, HTTP 400 otherwise). Blank actor keeps the bounded top-20 overview; an actor with no recorded events shows zero, not a missing account assertion.

Admin-only permissions apply to HTML, JSON and CSV. Existing CSV formula defenses are retained. No authentication role changes, money/HR performance claims or staff/account writes are introduced.
