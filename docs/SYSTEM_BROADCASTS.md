# Thông báo toàn hệ thống

Roadmap 086, phần thông báo website 026 và audit 090. Không gửi email, không dùng worker/queue trong đợt này.

## Luồng sử dụng

Admin vào Quản trị → Thông báo hệ thống, soạn tiêu đề 4–120 ký tự, nội dung 10–500 ký tự, chọn tất cả tài khoản hoặc một nhóm USER/STAFF/ADMIN. Chỉ tài khoản đang hoạt động tại thời điểm gửi mới nhận. Lưu bản nháp không tạo thông báo cho khách hàng.

Trang xem trước hiển thị nội dung, nhóm nhận và số tài khoản hiện tại. Admin xác nhận gửi qua form có CSRF và hộp thoại xác nhận. Thông báo sau khi gửi bất biến, không thể thu hồi; muốn sửa bản nháp thì hủy rồi soạn bản mới. Dữ liệu người nhận là nhóm ở thời điểm gửi, không phải danh sách tài khoản cố định từ lúc soạn. Nếu số lượng thay đổi so với lần xác nhận, trang báo lỗi và yêu cầu xem lại số mới.

Khách hàng nhận trong `/notifications`, có nhãn Thông báo hệ thống, hỗ trợ chưa đọc/đã đọc và số đếm trên thanh điều hướng. Đánh dấu đã đọc vẫn theo từng chủ tài khoản. Người đăng ký sau khi gửi không nhận hồi tố. Nội dung được escape, không render HTML. Không đưa thông tin riêng tư hoặc bí mật vào nội dung chung.

## Quyền, dữ liệu và giao dịch

- `/admin/broadcasts`, `/new`, `/{id}`: chỉ ADMIN; STAFF và khách không có quyền.
- Các POST tạo/gửi/hủy còn kiểm tra trạng thái và quyền ADMIN từ database, không chỉ dựa vào role trong phiên. Tạo tối đa 10 bản nháp/giờ, xác nhận gửi tối đa 5 lần/15 phút theo Admin trên từng instance.
- Tối đa 5.000 người nhận/lượt. Nhóm trống/quá giới hạn không được gửi. Không tuyên bố hỗ trợ gửi hàng triệu người hoặc email.
- Gửi dùng khóa dòng bản nháp và một transaction: INSERT SELECT tạo thông báo, đánh dấu SENT, ghi sự kiện audit. Số dòng chèn phải bằng số đã xác nhận, nếu không rollback toàn bộ. Lỗi database cũng rollback. Retry/gửi đồng thời cùng mã đã SENT chỉ trả kết quả cũ, không gửi thêm.
- Unique `(broadcast_id, owner_username)` là lớp bảo vệ database bổ sung. Thông báo cũ có broadcast_id NULL tiếp tục hoạt động.
- Bản nháp DRAFT có thể CANCELLED; hủy lặp không thêm sự kiện. SENT không được hủy. Lưu người tạo, người gửi/hủy, thời gian UTC, số người nhận thực tế và lịch sử CREATED/SENT/CANCELLED. Mỗi bản tối đa hai sự kiện (tạo và gửi hoặc hủy); danh sách bản nháp/thông báo phân trang 20 dòng, giữ bộ lọc trạng thái.

V14 thêm `system_broadcasts`, `broadcast_events`, khóa ngoại và index; thêm cột nullable/unique trên `account_notifications`. Không sửa nội dung thông báo cũ hoặc schema migration trước đó. PostgreSQL production dùng Flyway/Hibernate validate; H2 development dùng schema update. Sao lưu trước triển khai. Gửi đồng bộ phù hợp nhóm nhỏ; triển khai lớn cần queue/outbox và xử lý nền có chống trùng riêng.

## Kiểm thử

- `BroadcastIntegrationTest`: bản nháp chưa gửi, nhóm role/enabled, không gửi hồi tố, gửi lặp, CSRF/role thực trong database, quyền Staff/User, thay đổi số người nhận, hủy, giữ form lỗi, escape XSS, lọc/phân trang, giới hạn 5.000 và tích hợp chưa đọc/đã đọc.
- `PostgresBroadcastDeliveryTest`: hai Admin gửi đồng thời chỉ tạo một bản cho mỗi người nhận; trigger trong database CI cố ý bỏ một dòng để kiểm tra rollback toàn bộ fan-out, trạng thái và audit, rồi thử lại thành công sau khi gỡ trigger.

Chạy bằng Java 17 và PostgreSQL test database theo hướng dẫn build. Nghiệm thu trình duyệt gồm soạn sai → lưu bản nháp → xem trước → xác nhận gửi → khách nhận/đánh dấu đã đọc → Admin lọc và hủy bản nháp; kiểm tra 320/390/768/1440 px.

Kết quả cloud 2026-10-18: `clean verify` 135 test qua, không skip; sau bổ sung kiểm tra nhóm 5.001 người, chạy lại 8 test broadcast/H2/PostgreSQL đều qua. Trình duyệt Chromium kiểm tra luồng soạn lỗi/giữ form, xem trước, xác nhận gửi, đúng nhóm, Staff bị chặn, retry không trùng, khách đánh dấu đã đọc, hủy và lọc trạng thái: qua 16 lượt viewport. Thêm tiêu đề liền 120 ký tự và nội dung nhiều dòng 481 ký tự: qua 4 viewport 320/390/768/1440 px; nội dung phía khách giữ xuống dòng. Không lỗi JavaScript; app khởi động lại health UP và smoke auth/CSRF qua. Dữ liệu thử trong database cô lập, chưa phải nghiệm thu production.
