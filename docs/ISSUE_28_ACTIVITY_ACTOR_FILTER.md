# Issue #28 — Admin activity actor filter

`GET /admin/activity?area=ALL&actor=<username>` filters the bounded, four-source Admin audit timeline by **exact actor**, with the actor condition applied inside each database query *before* the 50-row cap. Valid names match `[a-zA-Z0-9._-]{3,100}`; malformed values fail HTTP 400. Missing/blank actor preserves the previous 50-most-recent behavior.

`GET /admin/activity/export.csv?area=ALL&actor=<username>` exports the same filtered event set. Existing CSV escaping/no-store behavior remains unchanged. Only fixed JPQL fields/order are used; no user SQL fragments or arbitrary sort. The UI retains the filter in its export link.

MockMvc regressions verify cross-source filtering, 55 distracting events not hiding a relevant older event, matched CSV, invalid actor, anonymous/STAFF/USER denial. Read-only; the existing account, catalog, assignment and order-status audit writes are not changed.

A unified timeline is NOT a tamper-evident or exhaustive audit archive; release requires PostgreSQL CI + CodeQL, human review and browser UAT.
