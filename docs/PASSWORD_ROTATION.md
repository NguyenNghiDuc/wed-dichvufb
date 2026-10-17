# GrowLab W1 — Đổi mật khẩu và thu hồi phiên cũ

## Chức năng

- GET `/account/password` chỉ khi đăng nhập; form xác nhận mật khẩu hiện tại và mật khẩu mới.
- POST `/account/password` yêu cầu CSRF, tài khoản từ database, kiểm tra mật khẩu hiện tại bằng BCrypt, mật khẩu mới ít nhất 12 ký tự và tối đa 72 byte UTF-8, trùng xác nhận, khác mật khẩu hiện tại.
- Khóa bản ghi tài khoản bằng PESSIMISTIC_WRITE khi thay đổi; giới hạn tối đa 10 lần thử đổi trong 1 giờ/tài khoản trên mỗi instance.
- Thay đổi lưu hash BCrypt, không bao giờ log/echo nội dung mật khẩu. Đổi thành công đăng xuất phiên hiện tại.
- Các phiên đăng nhập khác bị thu hồi trên lần gọi endpoint bảo vệ tiếp theo bằng cách so digest SHA-256 của bcrypt hash hiện tại với revision của AccountPrincipal. Có thể mở lại trang login công khai.
- Không cần migration vì thay đổi chỉ sử dụng users.password. Không yêu cầu SMTP/OAuth thật.

## Giới hạn trước production

- Limiter là in-memory mỗi instance; production nhiều node cần Redis/rate-limit edge.
- Thu hồi phiên được kiểm tra **trên request sau đó**, không chủ động phát broadcast để xóa phiên tại mọi node.
- Có nguy cơ phiên stale chưa bị kiểm tra nếu endpoint public cố ý bypass AccountStatusFilter; các endpoint riêng vẫn được bảo vệ.
- Kiểm tra staging với HTTPS, logout, back/forward, nhiều trình duyệt, role change và race cùng lúc.
- Giao diện hồ sơ nằm trong W2; tránh sửa trong W1 khi nhiều luồng cùng chạy. Có thể truy cập form trực tiếp tại /account/password. Đề nghị W2 thêm link nếu cần.
- Không phải tính năng quên mật khẩu qua email. Reset email/OTP/2FA cần cấu hình và duyệt riêng.
