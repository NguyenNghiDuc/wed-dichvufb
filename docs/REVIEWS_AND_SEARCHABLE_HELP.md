# Đánh giá dịch vụ và tìm kiếm trợ giúp

Roadmap 023 và phần tìm kiếm 027. Nhận xét dùng để cải thiện dịch vụ, không tự tạo đánh giá hoặc công bố nhận xét riêng lên trang bán hàng.

## Đánh giá

- Đơn tư vấn và đơn TikTok đã `COMPLETED` có liên kết Đánh giá / xem đánh giá dịch vụ trong chi tiết đơn.
- GET/POST `/reviews/CAMPAIGN/{id}` và `/reviews/TIKTOK/{id}` yêu cầu đăng nhập; chỉ chủ đơn được truy cập. Đơn người khác/không tồn tại trả 404; trạng thái chưa hoàn thành trả 400. CSRF bắt buộc khi gửi.
- 1–5 sao, nhận xét 10–1.000 ký tự sau trim. Nội dung hiển thị bằng văn bản, không render HTML; không cho ký tự điều khiển ngoài xuống dòng/tab. Form giữ nhận xét khi nhập sai; không giữ bí mật hay dữ liệu thanh toán.
- Một đánh giá bất biến cho mỗi đơn. Khóa dòng đơn khi gửi và ràng buộc UNIQUE tại PostgreSQL ngăn hai request ghi trùng. Service trả conflict 409; giao diện chuyển về đánh giá hiện có với thông báo, không ghi đè.
- Chỉ khách sở hữu đơn và ADMIN được xem nhận xét. `/admin/reviews` phân trang 20 dòng, lọc số sao/loại đơn, giữ lọc khi đổi trang. STAFF không có quyền xem trang này. Admin không được sửa/xóa nhận xét trong đợt này.
- POST tối đa 10 lần/15 phút theo tài khoản trên mỗi instance (không thay thế rate limit phân tán).

V13 thêm `service_reviews`, khóa ngoại tới đơn tư vấn/TikTok, đúng một loại đơn trên mỗi dòng, UNIQUE từng đơn, CHECK số sao/độ dài và index thời gian. Không thay đổi đơn cũ hoặc thanh toán.

## Trợ giúp

`/help?q=mat+khau&category=account` công khai, tìm trong tiêu đề/nội dung không phân biệt hoa thường, dấu tiếng Việt hoặc đ/d. Tất cả từ khóa phải xuất hiện; ký tự đặc biệt là văn bản thường. Từ khóa tối đa 120 ký tự, nhóm chủ đề account/orders/payments/support; giá trị không hợp lệ trả 400.

13 câu hỏi, bộ lọc chủ đề, xóa lọc, số kết quả, trạng thái không tìm thấy và liên kết hỗ trợ. Câu trả lời mở bằng details/summary sử dụng bàn phím, không cần JavaScript. Hướng dẫn phản ánh thực tế: email OTP và cổng thanh toán thật chưa kết nối, đơn chưa tự chạy qua nhà cung cấp.

## Kiểm thử

`ReviewAndHelpIntegrationTest`: ownership, CSRF, trạng thái hoàn thành, dữ liệu sai/giữ form, nhận xét trùng/XSS, cả hai loại đơn, quyền admin và tìm FAQ không dấu/kết hợp chủ đề.

`PostgresReviewConcurrencyTest`: hai yêu cầu đồng thời chỉ có một đánh giá được ghi; request còn lại bị conflict. Build đầy đủ dùng Java 17, PostgreSQL/Flyway production và H2 theo hướng dẫn repository. Kiểm tra trình duyệt gồm form lỗi → gửi → xem lại → gửi trùng, bộ lọc Admin, FAQ bằng bàn phím và màn hình 320/390/768/1440 px.

Kết quả cloud 2026-10-18: `clean verify` 128 test qua, không skip; bản cuối chạy lại 5 test đánh giá/FAQ và PostgreSQL đồng thời đều qua. Chromium kiểm tra nhập sai/giữ form, gửi/xem lại, chặn chủ đơn khác, bộ lọc Admin/liên kết đơn, tìm không dấu, bàn phím mở FAQ, không có kết quả/xóa lọc và 12 lượt viewport 320/390/768/1440 px: tất cả qua, không lỗi JavaScript. App khởi động lại health UP; đăng ký/đăng nhập/CSRF smoke test qua. Dữ liệu nghiệm thu trong database cô lập, không phải nghiệm thu production.
