# Issue #26 — customer progress timeline

The customer order details now present four internal milestones (pending review, approved, in progress, complete) in a responsive stepper. Separate terminal statuses REJECTED or CANCELLED are marked as closed **without** a misleading completed status. Percentages reflect workflow stages only, not actual delivery/ads metrics.

`GET /api/v1/campaign-orders/{id}/progress` requires the owner session, returns HTTP 404 for foreign/unknown orders, `Cache-Control: no-store` and never mutates an order.

Unit checks cover lifecycle stages and closed outcomes; MockMvc checks authentication, ownership, progress updates and HTML. No change to staff/admin operations, payments, prices or schema.
