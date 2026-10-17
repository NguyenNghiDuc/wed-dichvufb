# GrowLab — quy chuẩn code sạch và bảo mật

## Definition of Done (cho từng mục của ROADMAP_105.md)

- Có acceptance criteria định lượng; nêu chức năng ảnh hưởng và nguy cơ.
- Thay đổi nhỏ và tập trung trên branch riêng, người review kiểm tra diff.
- Java CI `mvn clean verify`: không lỗi compile/test; PostgreSQL + Flyway chạy thực trên CI.
- Có test happy path, validation, authentication, authorization/owner scope, CSRF, repeat/idempotency, concurrency khi liên quan.
- Có CodeQL + Dependabot, đọc và xử lý cảnh báo; scan xanh **không chứng minh không có lỗ hổng**.
- Không commit secrets, API tokens, dữ liệu cá nhân hoặc mật khẩu mặc định.
- OAuth của mỗi nền tảng do người dùng cấp phép; giới hạn rate/quota; chỉ gọi endpoint chính thức.
- Payment phải có provider-specific signature validation, idempotency, reconciliation, refund workflow trước production.
- Tài liệu hóa migrations + backup/restore + rollback trước cắt dữ liệu khách hàng.
- Manual UAT responsive, accessibility, latency, load, staging deploy. Chưa xác minh -> giữ unchecked.

## Thực trạng và giới hạn

- `TrafficLimiter` và `AuthenticationRateLimitFilter` hiện chỉ là **in-memory per instance**, không bảo vệ toàn bộ cluster. Muốn mở production công khai cần Redis/edge limiter.
- Cookie Secure được bật trong production profile, yêu cầu TLS reverse proxy.
- CodeQL chỉ là phân tích tĩnh; không thay thế kiểm thử xâm nhập, bảo mật hạ tầng, pentest và kiểm tra pháp lý.
- Các cổng thanh toán, tự đăng bài và OAuth Meta/TikTok vẫn chưa được tích hợp/duyệt.
- Backlog 105 mục chưa được đánh dấu completed: chỉ check [x] sau khi có các bằng chứng trên.
