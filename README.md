# GrowLab — Social Growth Platform (Spring Boot)

Dự án nâng cấp từ **wed-dichvufb**: phát triển kênh mạng xã hội bằng tư vấn hồ sơ, kế hoạch nội dung và chiến lược quảng cáo hợp lệ. **Không cung cấp hoặc cam kết follower giả**, chưa có tự động đăng bài, chưa kết nối API Facebook/TikTok/YouTube, chưa xử lý thanh toán thực tế.

## Current product scope

Trang chủ và luồng đặt mới chỉ nhận **tư vấn hồ sơ, nội dung và chiến lược quảng cáo**. `GET /tiktok/orders/new` và `GET /order/new` chuyển đến gói tư vấn; các POST tạo đơn cũ trả `410 Gone` sau kiểm tra đăng nhập/CSRF. Dữ liệu lịch sử, quyền tra cứu, hủy và xử lý đơn tồn vẫn giữ nguyên. Không có kết nối giao follower hoặc nhà cung cấp tương tác.

## Stack
Java 17, Spring Boot 3.2.5, Thymeleaf, Spring Security, Spring Data JPA, H2 (phát triển). Giao diện trắng–cam responsive. Nằm trong các thư mục Maven chuẩn `src/main/resources` và `src/test/java`.

## Codespaces và lỗi phiên bản Java

Dự án yêu cầu **JDK 17** và Maven 3.9.x. Nếu Maven báo `Detected JDK version 25 ... not in [17,18)`, terminal đang chọn sai JDK; giữ nguyên kiểm tra Enforcer trong `pom.xml`.

Trong Codespaces hiện có, sau khi cập nhật mã nguồn, chạy:

```bash
source scripts/use-java17.sh
java -version
mvn -version
mvn -DskipTests package
mvn spring-boot:run
```

Script chọn JDK 17 đã cài trong SDKMAN hoặc thư mục JDK chuẩn và cập nhật `JAVA_HOME`/`PATH` của **terminal hiện tại**. Nếu cần chỉ rõ đường dẫn: `source scripts/use-java17.sh /path/to/jdk-17`. Không chạy bằng `bash scripts/use-java17.sh` vì shell con không đổi môi trường terminal cha.

Nếu chưa có JDK 17, mở Command Palette và chọn **Codespaces: Rebuild Container** (hoặc **Dev Containers: Rebuild Container**). Cấu hình `.devcontainer/devcontainer.json` cài Java 17 và Maven 3.9.9 cho workspace mới/được rebuild, đồng thời đặt project runtime JavaSE-17 trong VS Code. Pull mã không tự đổi JDK của container đang chạy; mở terminal mới sau rebuild. Không ép Java language server mới chạy trên Java 17: extension có thể dùng JDK mới hơn, còn project/Maven phải dùng 17.

`-DskipTests package` chỉ kiểm tra build/đóng gói. `mvn clean verify` đầy đủ còn cần PostgreSQL test tại `localhost:5432`, database `growlab_ci` theo cấu hình trong Java CI; rebuild không tự tạo hoặc xóa database này.

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
- `GET /campaigns/export.csv?orderId=123&status=PENDING_REVIEW` — khách hàng tải CSV lịch sử chiến dịch của **chính mình** theo bộ lọc (bỏ tham số để xuất tất cả), tối đa 5000 dòng; CSV có BOM UTF-8 và escape công thức Excel. Nút tải nằm tại /campaigns. Không chứa dữ liệu khách hàng khác.

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

## Roadmap và kiểm soát chất lượng

- [Checklist 105 hạng mục](docs/ROADMAP_105.md) — chưa đánh dấu [x] nếu thiếu nghiệm thu bảo mật hoặc UAT.
- [Chính sách security QA](docs/SECURITY_QA.md) — Java/PostgreSQL CI, CodeQL, Dependabot, phân quyền, CSRF, bảo mật secrets và review.
- Support portal: `/support`, `/support/new`; Admin hỗ trợ: `/admin/support`.
- Hồ sơ cá nhân chỉ đọc: `/profile`; FAQ công khai: `/help`.
- Hủy đơn chưa duyệt: `POST /campaigns/{id}/cancel` (CSRF); REST: `POST /api/v1/campaign-orders/{id}/cancel`.
- Trang danh sách phân trang: `/campaigns?page=0`, `/admin/campaign-orders?page=0`, REST `/api/v1/campaign-orders/page?page=0&size=20`. Endpoint REST không phân trang cũ giới hạn 50 kết quả.
- Admin tải CSV: `/admin/campaign-orders/export.csv`, tối đa 5000 dòng, nội dung được escape.
- CI tạo coverage report JaCoCo để người review xem coverage; không có ngưỡng nào được coi là bằng chứng '100% không lỗi'.

Các tính năng chưa có OAuth/SMTP/2FA, chưa có API đăng bài thật, chưa có thanh toán VNPay và chưa deploy production. Chỉ đưa hệ thống lên công khai sau security review, migration, backup và kiểm thử thực tế.

## Admin user setup and configurable catalog (development/staging)

Do **not** ship a default admin password. First register a trusted account using `/register`; a database operator may explicitly grant `ADMIN` on the staging database using a reviewed one-time statement:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = '<trusted-account-username>' AND enabled = TRUE;
```

Then sign out and sign back in so the session receives the new role. Only authorized operators should have DB write access. Account management: `/admin/users`, audit: `/admin/users/audit`; service prices: `/admin/catalog`. Staff may access only `/admin/support/**`. Locking and role changes invalidate existing database-authenticated sessions on the following request. Admins cannot modify their own account via the UI.

Pricing comes from `service_offerings`, seeded via Flyway V6 on new PostgreSQL databases; development H2 uses matching enum defaults if the table is empty. Changing or disabling services affects **new orders only**, not historic order quotes. Pricing API: `GET /api/v1/services` (authenticated). External posting, fake engagement, and real payments remain disabled pending provider authorizations.

## Security hardening (October 2026)

Shared database rate limits, trusted-proxy configuration, account/source lockout, write budgets, deposit caps and chat restrictions are documented in [docs/SECURITY_HARDENING_2026_10.md](docs/SECURITY_HARDENING_2026_10.md). Apply migration V16 before deploying this version on PostgreSQL. This change does not enable wallet crediting or external fulfillment.
