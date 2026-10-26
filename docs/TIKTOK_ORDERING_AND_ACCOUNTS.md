# TikTok: server catalog, orders and account settings

## What is implemented

- `/`: public TikTok profile URL → country → server → quantity → calculated VND quote. Only enabled, configured servers appear. Empty catalogs disable ordering; no sample pricing or supplier claims are seeded.
- `/tiktok/orders/new`: authenticated quote review. POST `/tiktok/orders` revalidates URL, country, limits, enabled state and server revision. The server calculates prices; client-supplied prices are ignored. A changed server revision requires reviewing the new quote.
- `/tiktok/orders`: owner-scoped pagination and filters by order ID/status. `/tiktok/orders/{id}` includes the saved price/server snapshot, notes and paginated history. Only pending requests may be cancelled; a confirmation dialog is provided.
- `/admin/tiktok/servers`: ADMIN-only creation, editing and enabling/disabling servers. Country is ISO 3166-1 alpha-2; prices are integer VND **per follower**, not per 1,000. Unit prices: 1–1,000,000 VND; quantity bounds: 1–1,000,000. Maximum total: 10^12 VND. Disable rather than delete to preserve historical orders. Concurrent edits detect stale versions.
- `/admin/tiktok/orders`: ADMIN-only filtered and paginated request queue and details. Allowed internal transitions: PENDING_REVIEW → APPROVED → IN_PROGRESS → COMPLETED, or REJECTED while pending/approved. Customers may CANCEL only while pending. State changes are locked, recorded and produce owner notifications.
- `/profile`: edit display name, change password with current-password confirmation, revoke all sessions with current-password confirmation. Password changes use bcrypt and rotate the account authentication revision. Other sessions are rejected on their next request; current session is logged out. Settings password attempts are limited per account and application instance. Email changes, email recovery and OTP remain unavailable pending verified delivery.
- `/notifications`: owner-scoped pagination, unread filter, individual read and owner-scoped mark-all-read. State-changing endpoints require CSRF.

## Setup and operation

Java 17 and Maven >=3.8 remain required. H2 development uses Hibernate schema updates. PostgreSQL production uses additive Flyway V10 (account fields) and V11 (server/order/history tables), then Hibernate validation. Back up production before migrations; do not modify existing migration checksums. Authentication revision defaults to zero for existing accounts.

No default admin is created. Use the README's reviewed trusted-account promotion procedure, then sign back in. Go to `/admin/tiktok/servers`, enter accurate server details, prices and limits, and explicitly enable receiving requests. Until at least one server is enabled, the public form stays unavailable.

Orders are **manual requests**, not supplier submissions. There is no automatic fulfillment, balance debit, refund, real payment or guarantee of growth. The total is an estimate, not proof of payment. No supplier credentials are stored or requested by this module. Connect a real authorized provider in a separate integration before claiming automatic delivery. Never mark requests completed merely to produce public activity metrics.

The existing consulting campaign, support, content, staff and catalog workflows remain separate. Customer order/activity lists never show other customers' identities or links; the public counter is an aggregate of accepted requests.

## Validation

The test suite covers database-backed ordering, URL and amount validation, server states/revisions, historic price preservation, permissions, CSRF, status transitions, cancellation and pagination. Account tests check names, password validation/hashing and revocation. Notification tests check ownership and bulk-read behavior. PostgreSQL integration tests also validate migrations against the complete entity schema.

## Shared UI and workflow improvements

All account, public authentication/help, staff and admin templates share the dark pink/cyan theme and a role-aware navigation fragment. Public visitors see login/register; customer, staff and admin links are rendered according to authenticated roles. The unread badge uses the current owner's database count. `/site.js` provides accessible password visibility, byte-limit feedback, character counters, POST loading states and confirmation dialogs for destructive actions. Server-side authorization, CSRF and validation remain authoritative when JavaScript is disabled.

TikTok customer and admin lists now export their current ID/status filter as UTF-8 CSV with BOM, a 5,000-row cap, formula-safe cell escaping and `Cache-Control: no-store`. Customer export derives ownership from authentication; it accepts no owner override. Admin export requires ADMIN.

Customers may close their own resolved support request; repeated close is idempotent and other owners receive 404. Closed requests cannot accept replies. Invalid support, registration and content create/edit forms preserve non-secret submitted data. Registration validates the bcrypt 72-byte UTF-8 ceiling instead of relying only on character length. Passwords are never echoed into returned HTML.

### Tra cứu tài khoản và yêu cầu hỗ trợ

- `/admin/users`: tìm tên đăng nhập, tên hiển thị hoặc email (không phân biệt hoa thường), kết hợp quyền USER/STAFF/ADMIN và trạng thái hoạt động/đã khóa. Giữ bộ lọc khi chuyển trang, khóa/mở khóa hoặc đổi quyền; thao tác vẫn yêu cầu CSRF, quyền ADMIN và ghi nhật ký.
- `/support`: khách hàng tìm tiêu đề, mã yêu cầu và trạng thái trong yêu cầu của chính mình. Tham số `owner` không thể thay đổi phạm vi này.
- `/admin/support`: nhân viên và admin dùng các bộ lọc trên, thêm tên đăng nhập khách hàng chính xác.
- Từ khóa là chuỗi con thông thường: `%`, `_` và dấu gạch chéo ngược được tìm theo ký tự, không mở rộng thành mẫu SQL. Giới hạn từ khóa 120 ký tự, 20 kết quả/trang; trang ngoài 0–10000, mã không dương và bộ lọc sai trả HTTP 400. Không cần migration mới.
