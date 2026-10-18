# Issue #28 — staff-owned attention counts

The existing `/staff/overview` and `/staff/overview.json` now include two operational warnings:
1. Assigned `APPROVED` campaigns without another update for more than 72 hours.
2. Assigned `IN_PROGRESS` campaigns without an update for more than 7 days.

Counts use one grouped JPA query scoped by the **authenticated assignee** and read-only service; there is no arbitrary username input. Account enabled/STAFF/ADMIN validation remains unchanged. Historical test records restore `updated_at` after all JPA status changes to avoid the real `@PreUpdate` hook overwriting fixture ages.

The HTML offers category links to the existing STAFF queue. Those links filter status, **not** stale age, and the UI explicitly says so. These are review reminders, NOT guaranteed SLAs or new workflow state transitions.

Integration tests exercise overdue/fresh assignments, other staff isolation, forged username in JSON, HTML rendering and USER/anonymous denial. Requires CI at commit HEAD and manual browser UAT before acceptance.
