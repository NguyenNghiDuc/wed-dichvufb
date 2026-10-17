# GrowLab SLA vận hành nội bộ

- Campaigns are given UTC `due_at` upon creation: by default 24 hours for review.
- When ADMIN approves, `due_at` is refreshed: by default 72 hours for the processing period.
- Configure `GROWTH_SLA_REVIEW_HOURS` and `GROWTH_SLA_EXECUTION_HOURS` (each 1–720). **These are internal escalation targets, not a promise to customers.**
- Staff dashboard `/staff/campaign-orders` and Admin `/admin/dashboard` show live counts of overdue active campaigns; closed campaigns do not count.
- Flyway V8 adds `due_at` and `overdue_notified_at`. Historical in-progress orders are backfilled heuristically based on their last `updated_at` timestamp; review these when moving an existing dataset to production.
- Automatic in-app alerts are OFF by default. Operators can enable `GROWTH_SLA_ALERTS_ENABLED=true` after staging checks. Polling defaults to hourly (`GROWTH_SLA_SCAN_MS=3600000`).
- The scanner reads up to 100 overdue and not-yet-notified orders per run. Row locks and a persisted notification timestamp prevent duplicate notifications on concurrent nodes. Failures keep the event eligible for retry by transaction rollback.
- An order receives at most one overdue notification per deadline. Approval moves to a new deadline and resets the notification flag.
- The scheduler does **not** send email, charge payments, or execute external social-media actions. Use worker monitoring and a durable queue for high-volume production.
- Dates on dashboards are explicitly labeled UTC. Human reviewers should confirm timezone expectations with stakeholders.
- Test changed lifecycle statuses, retries, distinct owners, inactive users, migrations, scheduler concurrency, UI mobile views and alert volumes before marking roadmap items complete.
