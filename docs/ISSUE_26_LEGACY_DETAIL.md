# Issue #26 — read-only legacy order details

Customers can click an ID on the paginated legacy order list to view `GET /order/{id}`. The detail controller delegates to an owner-scoped repository query and returns HTTP 404 both for foreign and nonexistent IDs. Query string `owner` or user-provided IDs cannot override the authenticated principal.

The read-only responsive page shows service/link/quantity/server-defined unit price/total/creation time/notes and clearly states there is no automatic provisioning, progress or payment for legacy records. Inputs are escaped by Thymeleaf; there is no mutation endpoint.

Integration tests cover owner detail, cross-owner denial, missing ID, navigation and anonymous request.
