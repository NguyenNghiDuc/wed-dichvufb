# Deploy GrowLab staging with Render HTTPS + Casso V2 callback

**Status: infrastructure-as-code prepared, NOT deployed to an account.** Both money-handling flags default OFF. This setup uses PAID Render compute and paid Render Postgres; review pricing and approve it explicitly in the Render dashboard before provisioning. No charges or services were created by committing this file.

## Why Render?
GrowLab uses a Java 17 / Spring Boot Dockerfile, PostgreSQL 16 + Flyway and requires a public TLS endpoint. Render supports a Docker web service, private database linking and a public `onrender.com` HTTPS hostname.

## Create a staging deployment
1. In the Render account owned by the project, connect GitHub repository `NguyenNghiDuc/wed-dichvufb` and select the branch `feature/techcombank-wallet-settlement-20261021`.
2. Select **New > Blueprint**, choose that repository/branch and root `render.yaml`.
3. **Review paid resources before approving**: web `growlab-payment-staging` (1 CPU / 2 GiB) and database `growlab-payment-staging-db` (0.5 CPU / 1 GiB; 15 GiB disk). Both are deployed in Singapore. The database is only reachable through Render's internal network (public IP allow-list empty).
4. In the Render dashboard, approve/provision the Blueprint. The repository contains no bank credentials. The private `RENDER_DATABASE_URL` is provided by Render's supported `connectionString` property; `DB_USERNAME` and `DB_PASSWORD` come from the separately supported `user` and `password` properties. `scripts/deployment-entrypoint.sh` strips URL credentials and builds the PostgreSQL JDBC URL, binding to `PORT` (10000 on Render).
5. Let the web service build with Docker, start `SPRING_PROFILES_ACTIVE=prod`, apply Flyway and pass `/actuator/health`. `autoDeployTrigger: off` means later pushes won't silently deploy a payment-sensitive branch.
6. Copy the **actual public HTTPS URL assigned by Render** (do not guess a hostname). Run `sh scripts/render-staging-smoke.sh https://ACTUAL_RENDER_HOST` to verify the health route, login page, access control and payment webhook is safely disabled. You can also inspect it manually:
   ```sh
   curl -fsS "https://ACTUAL_RENDER_HOST/actuator/health"
   curl -I "https://ACTUAL_RENDER_HOST/login"
   curl -sS -o /dev/null -w '%{http_code}\n' -X POST \
     -H 'Content-Type: application/json' -d '{}' \
     "https://ACTUAL_RENDER_HOST/api/v1/payments/webhooks/casso-v2"
   ```
   Expected: health contains `"status":"UP"`, login loads, and the unsigned/disabled webhook returns **503**. A 503 while OFF is a *safe lock*, not failed hosting. The login endpoint may redirect if accessed with HEAD; use GET when needed.
7. Configure staging database backup/restore testing. Do not import production money/customer data into the new DB. Review logs without logging payment secrets or bank data.

## Prepare a future Casso Webhook V2 registration
Casso console: `https://my.casso.vn` → **Kết nối** → **Tích hợp** → **Thêm tích hợp** → choose **Webhook V2** (NOT legacy Webhook). Select only the already-authorized Techcombank account as the receiving bank. Set the exact callback to:
```text
https://ACTUAL_RENDER_HOST/api/v1/payments/webhooks/casso-v2
```
- Casso generates a **Webhook V2 Key bảo mật**, used to sign payloads with `X-Casso-Signature`. Do not share the key in GitHub or chat. Set it securely in the Render dashboard as `GROWTH_CASSO_WEBHOOK_SECRET` only after connecting the service. It is NOT the legacy `secure_token` integration.
- Set `GROWTH_BANK_ACCOUNT_NUMBER` and `GROWTH_BANK_ACCOUNT_HOLDER` as secure Render environment variables **only after the bank account is approved/linked**.
- Keep `GROWTH_CASSO_ENABLED=false` and `GROWTH_LIVE_PAYMENTS_READY=false` while provisioning hosting. They intentionally block signed event acceptance and hide QR. Do not run Casso's test delivery expecting a 200 while these flags are OFF.
- Register and activate the webhook only once bank/provider authorization, staging test plan and required operator/reconciliation controls are in place. For Casso's **Gọi thử** and **Strict mode** to pass, a correctly signed request must return HTTP 200 with `success: true`. The live processor should be enabled only as part of a controlled integration test with a bank statement-confirmed transaction. A Casso demo call alone is not a verified payment.
- Verify that the **webhook URL is HTTPS**, public, and returns a response within 5 seconds. Casso may retry failures. Never paste banking passwords, OTP, Casso secret or Render database credentials into the repository or support messages.

## Deployment gate
- Do **not** set the two payment feature flags TRUE until connected, reviewed and end-to-end tested against a real authorized bank statement. A public HTTPS website alone does **not** mean deposit payments are live.
- Do not use this staging stack for live customer funds; separately plan a production deployment, financial compliance, reliable backups, security monitoring, manual reconciliation and customer refunds.
- Webhook endpoint accepts POST only. It rejects missing/invalid Casso signature. Test all invalid signatures and duplicate transactions.
- Explicitly check the existing PR #55 and current HEAD/CI before the final merge. Main is not automatically changed.

References:
- https://render.com/docs/blueprint-spec
- https://render.com/docs/health-checks
- https://developer.casso.vn/webhook/thiet-lap-webhook-thu-cong

## Local verification before deploying
- Run `sh scripts/test_deployment_entrypoint.sh` to test Render URL conversion and Compose fallback with a mock Java command (does not connect to any database).
- Run `render blueprints validate render.yaml` using the authenticated Render CLI for full provider schema and workspace validation. This requires Render access; ordinary YAML parsing alone is NOT authoritative validation.
- The Render Blueprint does not create any service until the account owner explicitly approves it in the Render dashboard. Select the GitHub feature branch; the main branch does not yet include these files.
