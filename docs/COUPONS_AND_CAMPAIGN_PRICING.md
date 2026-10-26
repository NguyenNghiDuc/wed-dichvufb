# Mã giảm giá cho đơn tư vấn

Roadmap 071, phần quản trị 087 và tính giá server 033. Áp dụng cho chiến dịch tư vấn/nội dung/quảng cáo; chưa áp dụng cho đơn TikTok, đơn cũ, ngân sách quảng cáo hoặc ví tiền. Không kết nối cổng thanh toán và không phát sinh chuyển tiền.

## Quản trị

Admin vào Quản trị → Mã giảm giá để tạo mã và xem điều kiện/lịch sử dùng. Mã chuẩn hóa viết hoa, 4–32 ký tự A–Z/0–9/gạch ngang/gạch dưới, không trùng với mã đã tồn tại. Có hai kiểu: giảm VND cố định hoặc phần trăm 1–100. Giới hạn giảm tối đa và giá trị đơn tối thiểu tính bằng VND; trần 0 nghĩa là không đặt thêm trần. Giảm thực tế luôn không vượt giá gốc, giá sau giảm không âm.

Điều kiện bất biến sau khi tạo. Admin có thể bật/tắt mã với CSRF, kiểm tra quyền ADMIN đang hoạt động trong database, xác nhận thao tác và kiểm tra trạng thái cũ để không ghi đè thao tác khác. Lưu audit người tạo/bật/tắt theo UTC, hiển thị 20 thao tác gần nhất. Danh sách mã và lịch sử dùng phân trang 20 dòng. Tạo tối đa 20 mã/giờ theo Admin trên từng instance.

Thời gian bắt đầu/kết thúc nhập theo UTC; bắt đầu bao gồm thời điểm đó, kết thúc không bao gồm. Có tổng lượt tối đa 100.000 và tối đa mỗi tài khoản 1–100 (không vượt tổng lượt). Không xóa hoặc tái sử dụng mã cũ, không sửa điều kiện làm thay đổi ý nghĩa lịch sử.

## Khách hàng và giá

Form `/campaigns/new` thêm mã tùy chọn, giá gốc, giảm dự kiến và giá sau giảm. JavaScript gọi GET `/campaigns/quote` để xem giá từ server, có trạng thái đang kiểm tra/mã lỗi, hủy request cũ và nút Kiểm tra mã để thử lại. Quote chỉ đọc, không giữ lượt; tối đa 60 lần/10 phút/tài khoản trên mỗi instance. Không gửi form nếu mã chưa kiểm tra xong hoặc không hợp lệ. Khi tắt JavaScript, mã vẫn được server kiểm tra lúc gửi; kiểm tra trong trang chi tiết sau khi tạo, không coi giá HTML trước gửi là giá cuối.

POST web và API `/api/v1/campaign-orders` chấp nhận `couponCode` tùy chọn; request cũ bốn trường vẫn hoạt động. Server lấy giá gói từ catalog rồi kiểm tra mã, hiệu lực, trạng thái, giá trị tối thiểu, tổng lượt và lượt theo tài khoản. Không tin trường giá/giảm giá do client gửi. Phần trăm làm tròn xuống VND, sau đó áp dụng trần và chặn ở giá gốc. Quote và form xác nhận chỉ là dự kiến; tại POST luôn tính lại theo trạng thái hiện tại. Mã hết hạn/hết lượt khi gửi sẽ trả lỗi và giữ thông tin form.

Một đơn dùng tối đa một mã. **Lượt dùng tính khi tạo đơn thành công; hủy hoặc từ chối đơn không trả lượt.** Quy tắc được hiển thị trước khi gửi để tránh nhầm với thanh toán/hoàn tiền. Các request tạo đơn riêng biệt dùng lượt theo hạn mức; không tự hứa idempotency của checkout ngoài giới hạn mỗi tài khoản.

Đơn lưu bản chụp giá gốc, số VND giảm, mã và giá sau giảm (`quotedPriceVnd`). Thay đổi bảng giá hoặc tắt mã không đổi đơn cũ. Trang chi tiết và JSON API hiển thị các trường này. CSV giữ định dạng cũ để tương thích; `price_vnd` là báo giá sau giảm, không phải doanh thu đã thu tiền.

## Giao dịch và migration

V15 thêm `coupons`, `coupon_uses`, `coupon_audit`, khóa ngoại/unique/check/index và các trường giá trong `campaign_orders`. Giá cũ được sao chép vào giá gốc, giảm mặc định 0; không đổi giá đã ghi nhận. Getter tương thích dữ liệu H2/fixture cũ có giá gốc NULL. Mật khẩu, ví và thông báo cũ không thay đổi.

Dùng row lock trên mã trong cùng transaction tạo đơn để kiểm tra/giữ lượt, tạo đơn, lưu lượt dùng và ghi sự kiện/thông báo. Nếu bất kỳ bước ghi nào lỗi, rollback toàn bộ, không mất lượt và không để lại đơn dở. Unique order_id bảo đảm một bản ghi dùng mã mỗi đơn. Row lock hoạt động trên PostgreSQL chung giữa các instance; rate limit HTTP vẫn là bộ nhớ từng instance.

## Kiểm thử

- `CouponIntegrationTest`: form Admin/UTC/giữ dữ liệu sai, viết hoa mã, xem trước không tiêu lượt, giá client bị bỏ qua qua web/API, làm tròn/trần/giá 0, snapshot giữ nguyên khi đổi bảng giá, hạn dùng/đơn tối thiểu/tắt mã, hạn mức mỗi người/tổng lượt, hủy không trả lượt, CSRF/phân quyền, audit và form checkout lỗi.
- `PostgresCouponAtomicityTest`: hai khách tranh lượt cuối chỉ một đơn dùng mã thành công; trigger lỗi khi ghi lượt dùng xác nhận rollback đơn/counter, gỡ trigger rồi thử lại thành công.

Nghiệm thu Chromium cần soạn mã → xem giá khi đặt đơn → xác nhận → kiểm tra snapshot/lượt dùng → thử lại đã hết lượt → tắt mã và kiểm tra UI, cùng lỗi mạng quote và màn hình 320/390/768/1440 px.

Kết quả cloud 2026-10-18: `clean verify` 144 test qua, không skip, gồm 6 test coupon/H2 và 2 test đồng thời/rollback PostgreSQL. Bản JS xử lý lỗi quote cuối đã kiểm tra cú pháp và nghiệm thu Chromium: form Admin sai/giữ mã, tạo mã, phần trăm có trần, quote không tiêu lượt, hộp xác nhận/đơn lưu snapshot, chặn hết lượt theo người dùng, tắt mã, lỗi mạng chặn gửi và nút thử lại khôi phục. 20 lượt viewport 320/390/768/1440 px đều qua, không lỗi JavaScript. App khởi động lại health UP và smoke auth/CSRF qua. Database nghiệm thu cô lập; chưa phải nghiệm thu production hoặc thanh toán thật.
