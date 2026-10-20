# Verified Techcombank transfer settlement – adapter contract (NOT live banking)

## Status
The website can show a configured Techcombank recipient and a unique transfer reference for each pending deposit; it has a wallet credit ledger, customer wallet history and a signed verified-settlement intake.
**No real Techcombank API, bank connection or approved payment provider is connected here.** Never claim auto-top-up is enabled until a bank/PSP provider integration has been approved, authenticated and tested with real settlement records.

## Configuration
- \`GROWTH_BANK_ACCOUNT_NUMBER\`: recipient Techcombank account digits only (6–24)
- \`GROWTH_BANK_ACCOUNT_HOLDER\`: actual recipient account name (required for transfer instructions)
- \`GROWTH_BANK_SETTLEMENT_SECRET\`: distinct random secret (>=32 characters) shared ONLY with a trusted bank/PSP verification adapter. Do not reuse legacy \`GROWTH_WEBHOOK_SECRET\`.
- Production: TLS, PostgreSQL with Flyway, secrets manager, secure operations and provider IP allowlist where appropriate.

If bank account or secret is missing, the verified-settlement endpoint responds 503; no automatic credit is possible.

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
