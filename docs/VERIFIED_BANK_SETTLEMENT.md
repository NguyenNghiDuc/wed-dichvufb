# Verified Techcombank transfer settlement – adapter contract (NOT live banking)

## Status
The website can show a configured Techcombank recipient, VietQR (via img.vietqr.io) and a unique transfer reference for each pending deposit ONLY when the trusted-settlement secret is also configured; it has a wallet credit ledger, customer wallet history and a signed verified-settlement intake.
**No real Techcombank API, bank connection or approved payment provider is connected here.** Never claim auto-top-up is enabled until a bank/PSP provider integration has been approved, authenticated and tested with real settlement records.

## Configuration
- \`GROWTH_BANK_ACCOUNT_NUMBER\`: recipient Techcombank account digits only (6–24)
- \`GROWTH_BANK_ACCOUNT_HOLDER\`: actual recipient account name (required for transfer instructions)
- \`GROWTH_BANK_SETTLEMENT_SECRET\`: distinct random secret (>=32 characters) shared ONLY with a trusted bank/PSP verification adapter. Do not reuse legacy \`GROWTH_WEBHOOK_SECRET\`.
- Production: TLS, PostgreSQL with Flyway, secrets manager, secure operations and provider IP allowlist where appropriate.

If bank account or secret is missing, the verified-settlement endpoint responds 503; no automatic credit is possible. The UI hides transfer instructions until bank account, account holder AND settlement secret are configured. QR image is served by a third party (VietQR) so clients will send the transfer amount/reference to img.vietqr.io.

## Transfer / settle
1. Authenticated customer creates deposit at \`/deposit/new\` (1,000–1,000,000,000 VND).
2. Open \`/deposit/{id}\`, show Techcombank account, correct amount, unique content \`GLNAP{id}\`.
3. Independently verify settled INCOMING credit through an approved bank/PSP API, NOT screenshots, customer-submitted notifications, browser calls or emails.
4. The trusted adapter validates its provider-specific signature, credited bank account, positive amount, final settlement status and a unique bank transaction identity.
5. Adapter POSTs raw JSON to \`/api/v1/payments/webhooks/bank-settled\` with header \`X-GrowLab-Signature\` = lowercase HMAC-SHA256 hex over raw UTF-8 JSON bytes using \`GROWTH_BANK_SETTLEMENT_SECRET\`.
6. Example authenticated adapter payload:

\`\`\`json
{"eventId":"bank-event-001","transactionId":"txn-unique-001","amountVnd":50000,"destinationAccount":"123456789012","description":"GLNAP123","status":"SETTLED"}
\`\`\`

7. Webhook requires exact recipient, amount, unique deposit code, pending deposit and settled status; creates immutable wallet credit + receipt + notification and marks APPROVED in one database transaction. Duplicate bank transaction id yields duplicate=true only if it is the *same* deposit/amount.
8. Mismatched / extra payment, wrong transfer content, cancelled or already approved deposit is NOT automatically credited. Operator must investigate against bank source and arrange correction/refund if applicable. Failed webhooks must be monitored and manually reconciled with provider data.

## Security / limits
- This signed endpoint is NOT public bank authentication and must not be exposed to arbitrary integrators; a compromised webhook secret can create false credits.
- Previous \`/api/v1/payments/webhooks/internal\` remains backward-compatible and STILL DOES NOT CREDIT WALLET. An approved legacy request cannot automatically be credited a second time.
- Wallet currently has **credits only**. Do not enable order purchases/debits, withdrawals or refunds until debit-side locking, ledger entries and refund controls are built/tested.
- Keep account details out of Git; do not edit old Flyway files. V40 migration is forward-only.
- Verify \`mvn -B clean verify\` (with Postgres test DB) and staging with a REAL verified provider adapter before production.


## Casso Flow V2 provider adapter (implemented, OFF by default)
- Casso documents Techcombank statement synchronization via its Flow product; it is a Casso read-only/RPA data connector, **not a public Techcombank API or a confirmed supported individual account**. Confirm provider eligibility and bank contractual terms before connecting.
- Use Casso **Webhook V2** only. URL: `https://YOUR_DOMAIN/api/v1/payments/webhooks/casso-v2`.
- Configure `GROWTH_BANK_ACCOUNT_NUMBER`, `GROWTH_BANK_ACCOUNT_HOLDER`, `GROWTH_CASSO_WEBHOOK_SECRET` (Casso V2 secret key), `GROWTH_CASSO_ENABLED=true` and, **after** approval + end-to-end test, `GROWTH_LIVE_PAYMENTS_READY=true` in deployment secrets (never in Git). Leave both flags OFF until all prerequisites pass.
- Casso V2 signature: `X-Casso-Signature: t=<milliseconds>,v1=<hex>`; canonical JSON of all recursively alphabetically sorted payload keys; HMAC-SHA512 over `timestamp + '.' + canonical_json`. Webhook rejects missing/invalid signature and timestamps outside 26 hours past / 5 min future. Key stays secret; never expose in browser or commit it.
- Requires `error=0`, positive incoming `data.amount`, `data.bankAbbreviation=TCB`, `data.accountNumber` matching configured recipient and `data.id>0` as provider transaction identity. It uses `data.description` for unique `GLNAP{id}` reference. Unique ledger constraints and row locking prevent duplicate wallet credits.
- Signed Casso demo/test deliveries are NOT proof that real Techcombank synchronization works. Manually reconcile at least one confirmed small bank transfer in staging before enabling public deposits. Casso Flow may have provider connection fees and availability constraints.
- `/api/v1/payments/webhooks/bank-settled` (internal adapter) is now explicitly disabled unless `GROWTH_INTERNAL_VERIFIED_ENABLED=true`, and MUST remain disabled when using Casso directly.
- Casso signature reference: https://github.com/CassoHQ/casso-webhook-v2-verify-signature and https://developer.casso.vn/webhook/thiet-lap-webhook-thu-cong.

## Admin reconciliation board (read-only)
- `GET /admin/deposits/reconciliation` is ADMIN-only, paginated, and read-only.
- Shows verified wallet credit count/total, pending requests, pending older than 24 hours and APPROVED requests without any wallet credit (including legacy generic webhook).
- APPROVED without wallet credit is not bank payment proof. Verify directly with bank/Casso before any correction or refund.
- New requests redirect to `/deposit/{id}` so customers can see their unique reference. Actual bank QR remains disabled until live activation.
