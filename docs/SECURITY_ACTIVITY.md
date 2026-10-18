# W1 — Nhật ký bảo mật tài khoản

- GET `/account/security?page=0`: phân trang SQL 20 sự kiện/trang của chính tài khoản hiện tại theo `createdAt DESC, id DESC`. Giới hạn trang 0–10000 và kiểm tra lại AccountPrincipal với database, không nhận account ID từ query.
- Ghi lại 4 loại sự kiện: đăng nhập thành công, đăng nhập thất bại cho tài khoản đã tồn tại, đổi mật khẩu và thu hồi mọi phiên. Không lưu mật khẩu, token, cookie, User-Agent hay IP, nhằm giảm dữ liệu nhạy cảm.
- Ghi nhật ký đăng nhập sau xác thực bằng LoginAuditSuccessHandler; các thay đổi mật khẩu/thu hồi phiên ghi cùng giao dịch database và rollback nếu ghi sự kiện thất bại.
- Dữ liệu UTC; đăng nhập sai chỉ tạo sự kiện theo ID nếu tên tài khoản hợp lệ và đã tồn tại. Không phản hồi khác biệt cho tên không tồn tại; giới hạn đăng nhập đã được áp dụng sau CSRF để giảm spam. Không ghi IP, mật khẩu hoặc mật danh đăng nhập thử.
- Dùng Flyway repeatable migration `R__w1_account_security_activity.sql` với CREATE IF NOT EXISTS, không chiếm số phiên bản migration V10–V19 của W2–W6. Vì chưa merge/deploy bản V20 nên thay thế trước nghiệm thu; kiểm tra lại schema khi tích hợp main.
- Trang lịch sử hiển thị tối đa 20 mục mỗi lần xem; có thể mở các trang cũ hơn, trong khi CSV riêng vẫn giới hạn 200 mục. Retention 90 ngày phải được chủ dự án nghiệm thu trước production.
- Không phải danh sách thiết bị đang online hay chứng nhận xác thực đa yếu tố.

## Lưu giữ và xóa lịch sử

- Lưu tối đa theo thời gian **90 ngày** (mặc định) bằng cấu hình `GROWTH_SECURITY_ACTIVITY_RETENTION_DAYS`. Chỉ cho 1–3650 ngày để tránh vô tình tắt retention.
- Job UTC chạy 03:20 mỗi ngày khi `GROWTH_SECURITY_ACTIVITY_CLEANUP_ENABLED=true` (mặc định), xóa tối đa 500 sự kiện quá hạn mỗi lượt. Phần tồn đọng sẽ được xóa trong các lượt kế tiếp; nên theo dõi backlog trước khi production.
- Xóa theo ID bằng truy vấn có tham số, không ghép SQL từ dữ liệu người dùng. Không xóa sự kiện còn trong hạn. Có test retention tự động.

## Xuất file CSV

- GET /account/security/export cung cấp tối đa 200 sự kiện gần đây **chỉ của tài khoản hiện tại**.
- Server lấy `accountId` từ phiên đã xác thực; không nhận account ID từ query/URL.
- File có header `time_utc,event_kind`, dữ liệu chỉ gồm timestamp UTC và enum cố định, không xuất mật khẩu, token, IP, User-Agent. Response `Cache-Control: no-store`, `Content-Disposition: attachment`.
- Bản xuất không chứa toàn bộ lịch sử nếu có hơn 200 sự kiện; trang hiển thị 20 mục mỗi trang và có thể phân trang.
