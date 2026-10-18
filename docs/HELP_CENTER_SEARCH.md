# GrowLab Help Center — public searchable FAQ

## Scope
- `GET /help`: public, read-only searchable FAQs. No login, database writes, external APIs or migrations.
- `q`: optional, 0–80 characters after trimming, searched case-insensitively and without Vietnamese accents. Separate words must all match either the question or answer.
- `category`: one of `all` (default), `account`, `campaign`, `content`, `payment`, `support`.
- Invalid category or overly long query returns HTTP 400. Queries never reach a database or command interpreter.
- Page includes label-associated search/select, visible results count, `<details>` expand/collapse and a helpful empty state. GET query parameters make searches bookmarkable and work without JavaScript. Thymeleaf HTML-escapes echoed user input.
- Content only describes currently supported workflows. Payment language explicitly says real payments are not enabled.

## Manual acceptance
1. Open `/help` in an incognito window: 14 questions are accessible with no authentication.
2. Search `huy don`: one result regarding cancellation appears. Search `đơn hàng`: accent-insensitive results appear.
3. Filter `Thanh toán`: see two payment safety answers; do not see cancellation questions.
4. Combine `Tài khoản` and `OTP`: one result appears. Reset filters restores all questions.
5. Search for a nonexistent phrase: no matches and clear guidance. Tab through controls and toggle FAQ details using keyboard.
6. Verify `?category=invalid` and queries >80 characters return 400 (direct URLs, bypassing the browser's maxlength).
7. Verify `?q=%3Cimg%20src%3Dx%20onerror%3Dalert(1)%3E` renders text safely and never creates an `img` element.
8. Check mobile viewport, screen reader labels, logged-out state and the authenticated support link.

## Automated tests
`mvn -B -Dtest=HelpCenterSearchIntegrationTest test`, then full `mvn -B clean verify` and CodeQL CI. Review and staging signoff are still required before merge.

## Maintenance
Curated content is a static list in `HelpController`. Update answers if services change; do not make unverified claims about payments, automatic posting, account recovery or customer guarantees. Keep the legacy `/help` route and existing security policy intact.
