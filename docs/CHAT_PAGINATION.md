# Chat ownership and bounded pagination (roadmap #025/#093/#099)

**State:** Implemented for integration testing; not yet owner-accepted.

## API behavior
- `GET /chat/thread/{other}?page=0&size=20` returns a Spring Data `Page` of messages in **both directions** between the logged-in user and the named contact, newest first. The caller identity is never provided in a query parameter.
- The maximum page size is 50; `page` must be between 0 and 10,000 inclusive. Invalid input returns HTTP 400.
- The existing `GET /chat/conversation/{sender}/{receiver}` keeps one-direction filtering and rejects callers who are not either participant, but returns **only the latest 50** items.
- The existing `GET /chat/inbox/{receiver}` allows only the recipient and returns **only the latest 50** incoming items. Clients requiring more history should migrate to the paginated thread API.
- `POST /chat/send` still requires login + CSRF and ignores spoofed sender/ID fields in JSON.
- Uses deterministic `timestamp DESC, id DESC` ordering to make each page repeatable; concurrent writes can still move offset-based pages, so do not claim snapshot consistency.

## Database
- Flyway `V9__chat_query_indexes.sql` creates indexes for `(sender, receiver, timestamp DESC, id DESC)` and `(receiver, timestamp DESC, id DESC)` for PostgreSQL 16.
- No deletion, account mutation, external service call, or production credentials.

## Verification
- H2/MockMvc tests for both directions, cross-account isolation, invalid page/size, legacy response cap, CSRF and spoofing, anonymous denial.
- PostgreSQL CI tests assert Flyway indexes and scoped query behavior against real PostgreSQL.
- Java CI, CodeQL, JAR and Docker smoke required at the branch head before direct `main` update.
- Human review: older chat API consumers may rely on unlimited result history or ascending order; confirm compatibility and mobile UI before production use.
- Review JaCoCo, browser UAT, pagination under concurrent writes, audit/retention policy.
- Existing CodeQL CSRF graph-processing error in issue #13 is not resolved by this change.
