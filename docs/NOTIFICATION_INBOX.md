# Customer notification inbox (prototype)

GET `/notifications?page=0&unreadOnly=false&days=0`:
- Requires a session; `unreadOnly=true` filters the signed-in user's unread notifications only.
- Page bounds are 0..10000, fixed size 20, stable order `createdAt DESC, id DESC`.
- A page query is applied inside the database; no unbounded account list is loaded.
- Owner-scoped unread count and browser `Cache-Control: no-store`.
- Optional `days=7|30|90` limits the timeline (including unread-only) to the last N calendar days in the server local timezone. `days=0` (default) searches all history. Invalid values return 400; no per-user data crosses ownership boundaries.
- Paging and unread links retain the selected time window, while the unread count remains the owner's all-time unread count.

POST `/notifications/{id}/read`: only the owner may read their entry, otherwise 404.
POST `/notifications/read-all`: marks only the signed-in customer's currently unread entries read. An atomic database update keeps prior read timestamps intact and is safe to repeat. Both POST endpoints require the existing CSRF token.

Tests: `mvn -Dtest=NotificationsIntegrationTest,NotificationInboxIntegrationTest test`, followed by `mvn test`.
No production deployment or roadmap acceptance without CI and staging UAT.

## Tìm kiếm nội dung an toàn
- `q` tìm tiêu đề và nội dung, sau khi bỏ khoảng trắng hai đầu; tối đa 80 ký tự Unicode, từ chối control chars.
- Truy vấn Criteria có tham số, kèm ràng buộc owner trước, hỗ trợ kết hợp `days`, `unreadOnly` và phân trang 20 mục. Ký tự LIKE `%`, `_` và `!` được escape thành literal, không biến thành wildcard.
- Không dùng trực tiếp q để ghép SQL, ORDER BY hay redirect URL. Đường dẫn bộ lọc và phân trang giữ q bằng Thymeleaf URI encoding.
- Đây là tìm kiếm chuỗi trên dữ liệu của một chủ tài khoản; trước tải lớn cần đánh giá hiệu năng với PostgreSQL thực và cân nhắc full-text indexes theo kế hoạch W7.

## Trang chi tiết và giữ trạng thái bộ lọc
- `GET /notifications/{id}` chỉ tải `id + principal.username`; tài khoản khác nhận 404, anonymous phải đăng nhập.
- Trang chi tiết dùng Thymeleaf `th:text` cho mọi nội dung không tin cậy; không render HTML người dùng, trang có `Cache-Control: no-store`.
- Form đánh dấu một thông báo hoặc tất cả đều yêu cầu CSRF, giữ `page`, `days`, `unreadOnly`, `q` qua tham số được validate; unread-only sau read-all về trang đầu để không rơi vào trang rỗng.
- Đánh dấu đọc là idempotent, giữ nguyên thời gian đọc cũ. Không nhận chủ tài khoản từ client.
