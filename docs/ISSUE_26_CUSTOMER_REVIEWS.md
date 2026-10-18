# Issue #26 — completed-order customer review

- POST `/api/v1/campaign-orders/{id}/review` (session, CSRF), JSON `{"rating":5,"feedback":"Nội dung ít nhất 10 ký tự"}`; only a completed order's owner can create.
- GET `/api/v1/campaign-orders/{id}/review` (session), no-store, 404 for unknown or foreign order or no review. Response DTO contains only campaign ID, stars, text, and timestamp.
- Customer web order details `/campaigns/{id}`: readonly existing review or one-time form with CSRF.
- Reviews 1–5 stars, 10–500 characters; no customer-supplied owner, status, price, or publication controls.
- Exclusive row lock on campaign order and unique database constraint prevent duplicate reviews in concurrent submissions.
- Flyway V11 creates `campaign_reviews`, additive without changing Admin/chat/payments; H2 uses JPA mapping.
- CI and human review required before merge; stage should check performance and real browsers.
