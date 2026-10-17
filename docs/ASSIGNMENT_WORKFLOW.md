# Phân công nhân viên GrowLab

## Quyền và luồng
- Admin đã đăng nhập và **có role ADMIN đang hoạt động trong database** được gán, đổi hoặc bỏ phân công tại `/admin/campaign-orders`.
- Đơn được gán cho nhân viên **STAFF hoặc ADMIN đang hoạt động**. Không giao được đơn REJECTED/CANCELLED/COMPLETED.
- Admin vẫn chịu trách nhiệm duyệt PENDING_REVIEW → APPROVED; nhân viên chỉ được xử lý các đơn gán cho mình từ APPROVED → IN_PROGRESS → COMPLETED.
- Nhân viên xem `/staff/campaign-orders`, không xem được danh sách người khác hay cấp quyền Admin.
- Hành động được khóa giao dịch bằng JPA pessimistic lock, ghi `campaign_assignment_events`, có thông báo cho khách và người được giao.
- Đổi quyền hoặc khóa nhân viên sẽ chặn họ dùng các trang Staff và xử lý đơn; Admin cần chuyển lại các đơn chưa hoàn thành.
- Dữ liệu được thêm bằng Flyway `V7__campaign_assignments.sql`; không sửa migration cũ.

## Những việc cần trước production
- Staging/UAT với tài khoản STAFF thực tế (không dùng mock), kiểm thử nhiều nhân viên thao tác cùng lúc.
- Quy tắc SLA, cảnh báo việc trễ, đồng bộ thông báo email và hàng đợi sẽ được phát triển sau.
- Danh sách chọn nhân viên giới hạn 100 mục đầu tiên; cần tìm kiếm phía server khi số nhân viên vượt mức này.
- Các thay đổi không thu tiền thật hoặc điều khiển tài khoản mạng xã hội của khách.
