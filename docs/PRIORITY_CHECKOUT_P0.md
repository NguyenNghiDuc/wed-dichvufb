# Ưu tiên P0: 110, 113, 114, 115

Danh sách 106–125 trong `ROADMAP_105.md` đã được thay bằng danh sách/ưu tiên chủ dự án cung cấp; không giữ cách gán mã số của các đề xuất cũ.

## Báo giá và các bước đặt đơn tư vấn

- 110: chọn gói gọi `/campaigns/quote` kể cả không nhập mã giảm giá; hiển thị giá gốc, giảm và giá sau giảm từ server. HTML giá gói không được coi là kết quả quote cuối. Đang tải hoặc lỗi quote thì chặn gửi; có trạng thái chờ/lỗi và nút kiểm tra lại. Thay gói/mã hủy request cũ, bỏ phản hồi khác lựa chọn hiện tại. Quay lại trang qua history yêu cầu quote mới. POST vẫn tính lại giá/điều kiện, không giữ lượt mã khi xem trước.
- 113: form tư vấn chia ba bước Chọn gói → Nhu cầu → Xem giá và gửi. Có tiến độ/aria-current, nút tiếp/quay lại, kiểm tra trường bắt buộc trong từng bước và giữ dữ liệu khi quay lại. Không giấu fieldset nếu tắt JS, nên form vẫn có thể sử dụng và server vẫn kiểm tra mọi trường.
- 114: trước khi gửi có hộp xác nhận nền tảng, gói, link, mục tiêu, mã giảm, khoản giảm và giá dự kiến. Nút chỉnh sửa quay lại dữ liệu hiện tại. Chỉ sau xác nhận mới POST. Thông báo rõ đây là yêu cầu tư vấn và chưa thanh toán. Giá hoặc điều kiện thay đổi ở POST có thể làm request bị từ chối hoặc báo giá được cập nhật; không trừ tiền tự động.

Với TikTok, trang đầu → đăng nhập nếu cần → checkout đã có báo giá/link/quốc gia/server/số lượng để kiểm tra trước POST; đợt này bổ sung trang xác nhận thành công, không thay thế toàn bộ form TikTok bằng wizard tư vấn.

## Trang xác nhận thành công

- 115: POST web tư vấn chuyển tới `/campaigns/{id}/success`; POST TikTok chuyển tới `/tiktok/orders/{id}/success`.
- GET chỉ đọc đơn của principal hiện tại. Khách khác, kể cả ADMIN không phải chủ đơn, nhận 404; chưa đăng nhập được chuyển tới login. Không lấy mã đơn hoặc giá từ flash/query do client cung cấp.
- Hiển thị mã đơn, dịch vụ, link đã lưu, giá gốc/giảm/giá ghi nhận và trạng thái hiện tại. Tải lại hoặc mở lại trang không tạo thêm đơn hay tiêu lượt.
- Có liên kết chi tiết/tiến độ, lịch sử đơn và hỗ trợ. Thông báo ghi nhận yêu cầu không phải biên nhận thanh toán hoặc cam kết kết quả. Đơn có thể đổi trạng thái sau đó; trang luôn đọc trạng thái hiện tại, không tự khẳng định mãi là Chờ duyệt.
- API tạo đơn vẫn trả 201 + Location tới tài nguyên API, không chuyển JSON client tới trang HTML. Luồng hủy và đăng nhập giữ nguyên địa chỉ cũ.

Không cần migration database mới. Đây là bước hoàn thiện P0, không tự đánh dấu toàn bộ P1/P2 đã hoàn thành. Mục 121 đã có đánh giá riêng từ đơn hoàn thành; công bố đánh giá lên trang dịch vụ chưa triển khai.

## Kiểm thử

`PriorityCheckoutIntegrationTest`: hai trang success, kiểm tra ownership/Admin/anonymous/không tồn tại, refresh không tạo đơn, snapshot giá, quote không mã lấy catalog thật và gói bị tắt, form lỗi giữ thông tin. Các test checkout/coupon hiện có cập nhật kỳ vọng địa chỉ redirect mới; kiểm tra giá server/CSRF vẫn giữ.

Nghiệm thu trình duyệt: xác thực bước bắt buộc, tiếp/quay lại giữ dữ liệu, quote không mã và có mã, lỗi mạng/retry, hộp xem lại/chỉnh sửa, POST → success → refresh → chi tiết, TikTok GET checkout → POST → success, no-JS và viewport 320/390/768/1440 px. Trình duyệt/test database cô lập, chưa thay nghiệm thu production.

Kết quả cloud 2026-10-18: `clean verify` 149 test qua, không skip. Chromium nghiệm thu bước bắt buộc/tiếp/quay lại, giữ/chỉnh sửa nội dung, báo giá không coupon, hộp xác nhận, POST tới success, refresh không thêm đơn, chặn tài khoản khác, no-JS, lỗi mạng quote/retry và success TikTok: tất cả qua. 12 lượt viewport (320/390/768/1440 px) và hộp review trên mobile không tràn; không lỗi JavaScript. App khởi động lại health UP và smoke auth/CSRF qua.
