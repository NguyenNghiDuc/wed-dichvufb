# Issue #28 — visibility of STAFF order-progress audit

The Admin-only unified activity screen and filtered CSV now accept `area=STAFF_STATUS`. Rows are sourced from existing `CampaignEvent` records with the exact description `Nhân viên phụ trách cập nhật tiến độ`; the service already writes these in the same database transaction as the STAFF progress mutation and customer notification.

Admin status changes remain under `ORDER_STATUS`. CUSTOMER creation/cancellation events are never mislabelled STAFF/Admin actions. Exact actor filtering continues to apply inside the database query before the 50-row limit. No status mutation, auth policy change, schema migration or payment integration is introduced.

Tests cover both statuses, excluded customer events, actor filtering, Admin HTML/CSV, no-store and anonymous/STAFF/USER rejection. This is an operational activity timeline, not a tamper-evident complete audit ledger.
