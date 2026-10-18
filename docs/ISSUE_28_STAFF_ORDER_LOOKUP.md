# Issue #28 — assigned STAFF order ID lookup

`GET /staff/campaign-orders?orderId=123&status=APPROVED` supports exact order ID and optional status, with existing 20-row paging. A required `assigneeUsername` derived from the authenticated principal is applied in the database query. Foreign/not-assigned IDs yield an empty list, never returning order details.

`orderId` must be positive; malformed, negative and out-of-range paging values fail HTTP 400. Previous/next links preserve ID/status filter state. No mutation is introduced; the existing STAFF progress endpoint still uses a pessimistic assigned-order lock, CSRF protection and transactionally written CampaignEvent audit/notifications.

MockMvc tests cover own/foreign assignment, status combination, rejected invalid ID, cross-assignment progress 404 and USER/anonymous denial. This change only touches AdminStaffQueueRepository, CampaignAssignmentService, StaffCampaignController, staff template, tests and documentation.
