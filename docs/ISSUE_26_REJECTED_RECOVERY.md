# Issue #26 — safe recovery for rejected campaigns

The owner of a REJECTED campaign may open `GET /campaigns/{id}/copy` to prefill an **editable new request form** with original platform, service, link and brief. This is not an automatic retry or approval: no records are created by GET, and submitting the regular POST creates a fresh PENDING_REVIEW order with an updated server quote.

Foreign/unknown ID returns 404; a campaign in any other state returns 409. Rejected order remains unchanged and no refund/payment operations are performed. Existing service availability checks still apply. MockMvc tests cover ownership, status, form defaults and immutability.
