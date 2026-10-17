# GrowLab — Social Growth Platform (Spring Boot)

Dự án nâng cấp từ **wed-dichvufb**: phát triển kênh mạng xã hội bằng tư vấn hồ sơ, kế hoạch nội dung và chiến lược quảng cáo hợp lệ. **Không cung cấp hoặc cam kết follower giả**, chưa có tự động đăng bài, chưa kết nối API Facebook/TikTok/YouTube, chưa xử lý thanh toán thực tế.

## Stack
Java 17, Spring Boot 3.2.5, Thymeleaf, Spring Security, Spring Data JPA, H2 (phát triển). Giao diện trắng–cam responsive. Nằm trong các thư mục Maven chuẩn `src/main/resources` và `src/test/java`.

## Khởi động

```bash
mvn test
mvn spring-boot:run
```

Mở http://localhost:8080, đăng ký tài khoản có mật khẩu tối thiểu 8 ký tự, đăng nhập, chọn **Tạo chiến dịch**. Form được bảo vệ CSRF và server tự định giá. Chiến dịch mới có trạng thái `PENDING_REVIEW`. Không tự trừ tiền hay gửi dữ liệu sang bên thứ ba.

Mặc định dùng H2 file database tại `./data/fbdb` trong môi trường phát triển. Có thể cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Đừng dùng H2 mặc định hoặc `hibernate.ddl-auto=update` cho production; cần PostgreSQL, migrations và backup trước khi triển khai.

## API (cần đăng nhập và session + CSRF)
- `GET /api/v1/services` — các gói tư vấn, giá cố định trên server
- `GET /api/v1/campaign-orders` — chỉ các đơn của người dùng đang đăng nhập
- `GET /api/v1/campaign-orders/{id}` — chỉ đọc được đơn của mình
- `POST /api/v1/campaign-orders` — yêu cầu mới bằng JSON: `platform`, `serviceType`, `targetUrl`, `brief`
- `GET /api/v1/content-suggestions?platform=TIKTOK&topic=thoi%20trang` — gợi ý **theo mẫu**, không phải AI tạo sinh

Định dạng JSON mẫu:
```json
{
  "platform": "TIKTOK",
  "serviceType": "CONTENT_PLAN",
  "targetUrl": "https://www.tiktok.com/@example",
  "brief": "Tư vấn nội dung giáo dục phù hợp người xem trẻ."
}
```

Nền tảng: FACEBOOK, INSTAGRAM, TIKTOK, YOUTUBE. Gói dịch vụ: PROFILE_AUDIT, CONTENT_PLAN, AD_STRATEGY. Chỉ chấp nhận HTTPS từ domain tương ứng.

## Quản trị
- `GET /admin/campaign-orders` (cần quyền `ROLE_ADMIN`)
- `POST /admin/campaign-orders/{id}/status` (form CSRF) thay đổi trạng thái theo luồng: PENDING_REVIEW → APPROVED → IN_PROGRESS → COMPLETED; hoặc REJECTED khi còn chờ/đã duyệt.
- Đăng ký công khai **luôn** có role USER. Không có tài khoản admin mặc định. Chỉ người vận hành đáng tin cậy mới được gán role ADMIN trực tiếp trong database qua quy trình vận hành bảo mật.

## Những sửa lỗi đã thực hiện
- Khôi phục đúng đường dẫn resources (repo cũ có `src/main/ resources` với dấu cách thừa; giữ nguyên bản cũ để tránh mất dữ liệu).
- Giải quyết mapping /login và /register trùng.
- Bật xác thực và CSRF; mật khẩu mới dùng bcrypt.
- Không cho khách gửi `pricePerUnit` / `totalPrice` để sửa giá đơn cũ; gắn owner cho đơn cũ, yêu cầu nạp và chat.
- Không tự duyệt yêu cầu nạp hoặc ghi có tiền.

## Chưa hoàn tất / trước khi triển khai production
1. Đổi H2 sang PostgreSQL, thêm Flyway migrations và cấu hình môi trường.
2. Hoàn thiện xác thực email, giới hạn tốc độ, ghi log, khôi phục mật khẩu, audit Admin.
3. Kết nối OAuth/API **chính thức** của từng nền tảng theo quyền được cấp; không lưu mật khẩu tài khoản mạng xã hội. Thêm consent, quota, retry và kiểm thử.
4. Tích hợp AI tạo sinh qua nhà cung cấp được cấu hình an toàn; endpoint gợi ý hiện tại là template, **không** gọi LLM.
5. Thanh toán phải được xác minh bằng chữ ký webhook + đối soát, không tin dữ liệu gửi từ trình duyệt.
6. Kiểm thử end-to-end, accessibility, chống XSS/CSRF, xử lý tranh chấp đơn và backup production.
7. Xem lại dữ liệu / code legacy, dọn thư mục cũ và các file build đã commit (target/).

Không merge vào main trước khi CI/test được xác nhận.
