# Trung tâm trợ giúp – tìm kiếm FAQ (roadmap #027)

- `GET /help` là trang công khai, không cần đăng nhập và không gọi hệ thống thanh toán hay API bên ngoài.
- `q` (tối đa 80 ký tự) tìm trong câu hỏi + nội dung trả lời, hỗ trợ từ khóa tiếng Việt có/không dấu. Nhiều từ khóa được xử lý theo điều kiện tất cả các từ đều có mặt.
- `category`: all, account, campaign, content, payment, support; giá trị không hợp lệ trả HTTP 400.
- Nguồn trả lời là danh sách biên tập cố định trong `HelpFaqService`; không kết nối tới dữ liệu riêng tư của khách hàng.
- Kết quả dùng Thymeleaf `th:text` / `th:value` để escape dữ liệu không tin cậy; form GET không thay đổi trạng thái, không cần CSRF token.
- Có số lượng kết quả, thông báo không tìm thấy, liên kết đăng nhập và tạo ticket hỗ trợ.
- Không tuyên bố payment thật, tăng follower giả hoặc lịch nội dung tự đăng; cần cập nhật FAQ khi tính năng production thay đổi.

Kiểm thử: `mvn -Dtest=HelpFaqSearchIntegrationTest test`. Full CI kiểm thử Java/H2/PostgreSQL và smoke test Docker.
Đây là phần nâng cấp tính năng roadmap #027, chưa thay thế nghiệm thu giao diện mobile hoặc UAT.
