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

## Đăng xuất tất cả thiết bị (giữ mật khẩu cũ)

- GET /account/sessions hiển thị form bảo mật cho tài khoản đã đăng nhập.
- POST /account/sessions/revoke dùng CSRF và mật khẩu hiện tại; khóa account row.
- BCrypt hash được tái tạo với salt mới trên **cùng mật khẩu**, làm mới credential revision của account, không thay đổi mật khẩu mà người dùng phải nhớ.
- Tất cả phiên cũ (kể cả phiên hiện tại) không còn dùng được ở request riêng tiếp theo; POST thành công trả lại trang login.
- Giới hạn tối đa 10 lần thử/giờ/tài khoản trên mỗi instance. Không có khả năng đẩy lệnh đóng kết nối WebSocket ngay lập tức hoặc thu hồi session khi nó không gửi request; phần realtime cần session store/pubsub riêng.
