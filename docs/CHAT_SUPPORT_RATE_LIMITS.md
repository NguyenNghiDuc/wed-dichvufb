# Chat and support reply rate limits (Issue #27)

The existing per-process TrafficLimiter applies a fixed one-hour window per
authenticated sender:

- Chat sends: 120 per hour per sender.
- Customer ticket replies: 60 per hour per account.
- Staff ticket replies: 180 per hour per staff account.

Excess calls return HTTP 429 before the message and notifications are persisted.
Ticket replies are checked for ticket ownership/assignment first; all HTTP POST
routes still require session authentication and Spring CSRF protection. Limits
are not shared across application replicas; production multi-instance deployment
requires a centralized counter implementation, tuning for legitimate workloads,
metrics and appropriate retry guidance.

Integration tests: ChatSupportRateLimitIntegrationTest.
These controls do not make messages real-time; the chat UI is server rendered.
