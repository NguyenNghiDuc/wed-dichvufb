# W8 — chuẩn bị tích hợp thông báo email (chưa bật SMTP)

GrowLab hiện chỉ gửi notification bên trong website, và **không có mail credentials**.
Không giả lập rằng email đã được gửi hoặc tự kích hoạt email production.

## Cổng tích hợp khi có SMTP hợp lệ
1. Chủ dự án xác minh domain/SMTP, thiết lập SPF/DKIM/DMARC, quota, nội dung từ người gửi và chính sách opt-in/opt-out.
2. Chỉ dùng `NotificationDeliveryService` có phạm vi theo owner khi nhà cung cấp email đã được xác thực. Không gửi trực tiếp bên trong DB transaction của đơn hàng, tránh rollback gây email đã gửi ngoài ý muốn.
3. Dùng outbox bền vững với idempotency-key riêng từng event/recipient, retry exponential backoff, giới hạn tần suất và dead-letter. W7 cấp migration trước khi tạo outbox và W9 xác nhận integration tests.
4. Secrets đi qua quản lý biến môi trường/secret store (không commit username/password SMTP, OAuth refresh token).
5. Gửi email **không chứa dữ liệu nhạy cảm**; chỉ cung cấp tóm tắt và đường dẫn HTTPS đến chi tiết thông báo sau khi người dùng đăng nhập. Không coi email là bằng chứng chuyển trạng thái đã đọc.
6. Lưu ý bảo vệ spam/injection header, kiểm tra recipient đã xác minh và quyền riêng tư theo tài khoản.
7. Thêm kiểm thử fake SMTP, failure/retry, không gửi trùng, race khi tạo thông báo và không làm lộ nội dung giữa khách hàng; chỉ nghiệm thu staging trước khi bật thật.

## Hiện trạng
- Không có code SMTP thật hoặc khóa bí mật.
- Trạng thái đọc/chưa đọc lưu trong website và không bị thay đổi bởi bước chuẩn bị email.
- Hộp thư cá nhân, tìm kiếm và chi tiết không phụ thuộc mail server.
